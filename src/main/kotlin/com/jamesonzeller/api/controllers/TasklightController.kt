package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.tasklight.models.ParseRequest
import com.jamesonzeller.api.tasklight.services.ParseService
import com.jamesonzeller.api.tasklight.services.ParseServiceResult
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tasklight")
class TasklightController(
    private val parseService: ParseService
) {
    @PostMapping("/parse")
    fun parse(
        @RequestBody body: ParseRequest,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val userId = notionUserId(request)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Missing X-Notion-User-Id header"))

        return when (val result = parseService.parseTask(userId, body.text)) {
            is ParseServiceResult.Success -> ResponseEntity.ok(result.response)
            ParseServiceResult.DailyLimit -> ResponseEntity.status(429)
                .body(mapOf("error" to "Daily limit reached"))
            ParseServiceResult.UpstreamError -> ResponseEntity.status(502)
                .body(mapOf("error" to "Upstream error"))
            is ParseServiceResult.InvalidJson -> ResponseEntity.status(502)
                .body(mapOf("error" to "Invalid JSON from model", "content" to result.content))
        }
    }

    private fun notionUserId(req: HttpServletRequest): String? {
        return req.getHeader("X-Notion-User-Id")?.trim()?.takeIf { it.isNotBlank() }
    }
}
