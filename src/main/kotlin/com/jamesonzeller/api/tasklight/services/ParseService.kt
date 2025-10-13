package com.jamesonzeller.api.tasklight.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jamesonzeller.api.tasklight.models.ParseResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

private const val DAILY_LIMIT = 3

@Service
class ParseService(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    @Value("\${OPENAI_API_KEY}") private val openAiApiKey: String
) {
    private val zone: ZoneId = ZoneId.of("America/Chicago")
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    fun parseTask(userId: String, text: String): ParseServiceResult {
        val today = LocalDate.now(zone)
        if (isOverDailyLimit(userId, today)) {
            return ParseServiceResult.DailyLimit
        }

        val prompt = buildPrompt(today, text)
        val completion = callOpenAI(prompt) ?: return ParseServiceResult.UpstreamError
        val parsed = parseCompletionToTask(completion) ?: return ParseServiceResult.InvalidJson(completion)

        incrementDaily(userId, today)
        return ParseServiceResult.Success(parsed)
    }

    private fun isOverDailyLimit(userId: String, date: LocalDate): Boolean {
        val used = getUsed(userId, date)
        return used >= DAILY_LIMIT
    }

    private fun getUsed(userId: String, date: LocalDate): Long {
        val key = dailyKey(userId, date)
        return redis.opsForValue().get(key)?.toLong() ?: 0L
    }

    private fun incrementDaily(userId: String, date: LocalDate) {
        val key = dailyKey(userId, date)
        val newVal = redis.opsForValue().increment(key) ?: return
        setExpiryAtEndOfDayIfFirst(key, newVal, date)
    }

    private fun dailyKey(userId: String, date: LocalDate): String {
        return "user:$userId:tasklight:daily:$date"
    }

    private fun setExpiryAtEndOfDayIfFirst(key: String, newVal: Long, date: LocalDate) {
        if (newVal == 1L) {
            val endOfDay = date.atTime(23, 59, 59)
            val expiryInstant = endOfDay.atZone(zone).toInstant()
            redis.expireAt(key, Date.from(expiryInstant))
        }
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

    private fun callOpenAI(prompt: String): String? {
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

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer $openAiApiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null

        val tree = mapper.readTree(response.body())
        return tree.path("choices").path(0).path("message").path("content").asText("")
            .takeIf { it.isNotBlank() }
    }

    private fun parseCompletionToTask(content: String): ParseResponse? {
        return try {
            val node = mapper.readTree(content)
            val title = node.path("title").asText("")
            if (title.isBlank()) return null
            val dateNode = node.path("date")
            val date = if (dateNode.isMissingNode || dateNode.isNull) null else dateNode.asText()
            ParseResponse(title = title, date = date)
        } catch (ex: Exception) {
            null
        }
    }
}

sealed class ParseServiceResult {
    data class Success(val response: ParseResponse) : ParseServiceResult()
    object DailyLimit : ParseServiceResult()
    object UpstreamError : ParseServiceResult()
    data class InvalidJson(val content: String) : ParseServiceResult()
}
