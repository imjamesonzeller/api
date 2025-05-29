package com.jamesonzeller.api.mirror.notiontasks.models

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class NotionRequest(
    private var dbid: String,
    private var apiSecret: String,
    private var filter: NotionFilter
) {

    @Serializable
    data class RequestResponse(
        val results: List<JsonObject>
    )

    private fun request(): RequestResponse = runBlocking {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        println("📤 Sending request to Notion with filter: $filter")
        println("🔐 Using API key: ${apiSecret.take(5)}...")

        val response: HttpResponse = client.post("https://api.notion.com/v1/databases/$dbid/query") {
            header(HttpHeaders.Authorization, "Bearer $apiSecret")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("Notion-Version", "2022-06-28")
            setBody(filter)
        }

        val rawBody = response.bodyAsText()
        println("📥 Raw response body: $rawBody")

        Json {ignoreUnknownKeys = true}.decodeFromString<RequestResponse>(rawBody)
    }

    fun fetchTasks(): List<Task> {
        val requestResponse = request()
        return parseNotionResponse(requestResponse)
    }

    private fun formatDueDate(dueDate: String): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd")

        return try {
            val dateTime = ZonedDateTime.parse(dueDate)
            dateTime.format(formatter)
        } catch (e1: DateTimeParseException) {
            try {
                val localDate = java.time.LocalDate.parse(dueDate)
                localDate.format(formatter)
            } catch (e2: DateTimeParseException) {
                "Invalid Date"
            }
        }
    }

    private fun parseNotionResponse(response: RequestResponse): List<Task> {
        val tasks = mutableListOf<Task>()

        for (result in response.results) {
            val properties = result["properties"]?.takeIf { it !is JsonNull }?.jsonObject ?: continue

            val name = properties["Name"]
                ?.takeIf { it !is JsonNull }
                ?.jsonObject?.get("title")?.jsonArray?.getOrNull(0)
                ?.takeIf { it !is JsonNull }
                ?.jsonObject?.get("text")
                ?.takeIf { it !is JsonNull }
                ?.jsonObject?.get("content")
                ?.takeIf { it !is JsonNull }
                ?.jsonPrimitive?.content ?: continue

            val dueDateObj = properties["Due Date"]
                ?.takeIf { it !is JsonNull }
                ?.jsonObject?.get("date")
                ?.takeIf { it !is JsonNull }
                ?.jsonObject

            val dueDate = dueDateObj?.get("start")
                ?.takeIf { it !is JsonNull }
                ?.jsonPrimitive?.content
                ?.let { formatDueDate(it) } ?: "No Due Date"

            val priority = properties["Priority"]
                ?.takeIf { it !is JsonNull }
                ?.jsonObject?.get("select")
                ?.takeIf { it !is JsonNull }
                ?.jsonObject?.get("name")
                ?.takeIf { it !is JsonNull }
                ?.jsonPrimitive?.content ?: "No Priority Assigned"

            tasks.add(Task(name, dueDate, priority))
        }

        return tasks
    }
}