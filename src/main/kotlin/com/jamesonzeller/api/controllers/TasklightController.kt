package com.jamesonzeller.api.controllers

import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import org.springframework.beans.factory.annotation.Value
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jamesonzeller.api.tasklight.models.ParseRequest
import com.jamesonzeller.api.tasklight.models.ParseResponse
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.format.DateTimeFormatter

const val DAILY_LIMIT = 3

@RestController
@RequestMapping("/tasklight")
class TasklightController(
    private val redis: StringRedisTemplate,
    @Value("\${OPENAI_API_KEY}") private val openAiApiKey: String
) {
    private val zone: ZoneId = ZoneId.of("America/Chicago")

    private fun notionUserId(req: HttpServletRequest): String? {
        return req.getHeader("X-Notion-User-Id")?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun dailyKey(userId: String, date: LocalDate = LocalDate.now(zone)): String {
        return "user:$userId:tasklight:daily:$date"
    }

    private fun setExpiryAtEndOfDayIfFirst(key: String, newVal: Long) {
        if (newVal == 1L) {
            val endOfDay = ZonedDateTime.now(zone).toLocalDate().atTime(23, 59, 59)
            val expiryInstant = endOfDay.atZone(zone).toInstant()
            redis.expireAt(key, Date.from(expiryInstant))
        }
    }

    private fun getUsed(userId: String): Long {
        val key = dailyKey(userId)
        return redis.opsForValue().get(key)?.toLong() ?: 0L;
    }

    @PostMapping("/parse")
    fun parse(
        @RequestBody body: ParseRequest,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val userId = notionUserId(request)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Missing X-Notion-User-Id header"))

        val today = LocalDate.now(zone)
        if (isOverDailyLimit(userId, today)) {
            return ResponseEntity.status(429).body(mapOf("error" to "Daily limit reached"))
        }

        val prompt = buildPrompt(today, body.text)
        val mapper = ObjectMapper()

        val completion = callOpenAI(prompt, mapper)
            ?: return ResponseEntity.status(502).body(mapOf("error" to "Upstream error"))

        val parsed = parseCompletionToTask(completion, mapper)
            ?: return ResponseEntity.status(502).body(mapOf("error" to "Invalid JSON from model", "content" to completion))

        // Increment usage only after successful parse
        incrementDaily(userId, today)
        return ResponseEntity.ok(parsed)
    }

    private fun isOverDailyLimit(userId: String, date: LocalDate): Boolean {
        val used = getUsed(userId)
        return used >= DAILY_LIMIT
    }

    private fun buildPrompt(today: LocalDate, text: String): String {
        val weekday = today.dayOfWeek.name.lowercase().replaceFirstChar { it.lowercase() }

        return """
            You are a precise and reliable task parsing assistant. 
            Your job is to convert natural-language task descriptions into clean, structured data.

            Today's date is ${today.format(DateTimeFormatter.ISO_DATE)}. Today is a $weekday.

            When parsing dates:
            - Always interpret dates as referring to the **next upcoming instance in the future** (never in the past) unless the text clearly says “last” or “previous”.
            - Correct common spelling mistakes in weekday or month names (e.g., "firday" -> "Friday", "janury" -> "January").
            - If the intended date is ambiguous, choose the most **reasonable future** date based on context.
            - Use ISO 8601 format (YYYY-MM-DD) for all dates.

            Parse the following sentence: "$text".

            Ignore phrases like "remind me to", "remind me on", or similar expressions—only focus on the task and the date.

            Return only a JSON object in this exact format:
            { "title": ..., "date": ... }

            If no date is mentioned, set "date" to null.
            """.trimIndent()
    }

    private fun callOpenAI(prompt: String, mapper: ObjectMapper): String? {
        val http = HttpClient.newHttpClient()
        val root: ObjectNode = mapper.createObjectNode()
        root.put("model", "gpt-4o-mini")
        root.put("temperature", 0.0)
        val messages: ArrayNode = mapper.createArrayNode()
        messages.add(
            mapper.createObjectNode()
                .put("role", "user")
                .put("content", prompt)
        )
        root.set<ArrayNode>("messages", messages)

        val req = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer $openAiApiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
            .build()

        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() != 200) return null

        val tree = mapper.readTree(res.body())
        return tree.path("choices").path(0).path("message").path("content").asText("")
            .takeIf { it.isNotBlank() }
    }

    private fun parseCompletionToTask(content: String, mapper: ObjectMapper): ParseResponse? {
        return try {
            val node = mapper.readTree(content)
            val title = node.path("title").asText("")
            if (title.isBlank()) return null
            val dateNode = node.path("date")
            val date = if (dateNode.isMissingNode || dateNode.isNull) null else dateNode.asText()
            ParseResponse(title = title, date = date)
        } catch (e: Exception) {
            null
        }
    }

    private fun incrementDaily(userId: String, date: LocalDate) {
        val key = dailyKey(userId, date)
        val newVal = redis.opsForValue().increment(key)!!
        setExpiryAtEndOfDayIfFirst(key, newVal)
    }
}