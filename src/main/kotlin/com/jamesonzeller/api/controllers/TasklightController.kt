package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.tasklight.models.ParseRequest
import com.jamesonzeller.api.tasklight.models.NotionCompleteRequest
import com.jamesonzeller.api.tasklight.services.NotionOAuthService
import com.jamesonzeller.api.tasklight.services.OAuthException
import com.jamesonzeller.api.tasklight.services.ParseService
import com.jamesonzeller.api.tasklight.services.ParseServiceResult
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tasklight")
class TasklightController(
    private val parseService: ParseService,
    private val notionOAuthService: NotionOAuthService
) {
    @GetMapping("/notion/oauth/start")
    fun notionOAuthStart(request: HttpServletRequest): ResponseEntity<Void> {
        val binding = clientBinding(request)
        val result = notionOAuthService.startAuthorization(binding)
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(result.redirectUri)
            .build()
    }

    @GetMapping("/notion/oauth/callback", produces = [MediaType.TEXT_HTML_VALUE])
    fun notionOAuthCallback(
        @RequestParam("code") code: String?,
        @RequestParam("state") state: String?
    ): ResponseEntity<String> {
        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            return ResponseEntity.badRequest().body("Missing code or state parameter")
        }
        return try {
            val result = notionOAuthService.handleCallback(code, state)
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(result.html)
        } catch (ex: OAuthException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.TEXT_PLAIN)
                .body(ex.message ?: "OAuth error")
        }
    }

    @PostMapping("/notion/oauth/complete")
    fun notionOAuthComplete(
        @RequestBody body: NotionCompleteRequest,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        if (body.handoff.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Missing handoff"))
        }
        val binding = clientBinding(request)
        return try {
            val tokens = notionOAuthService.completeHandoff(body.handoff, binding)
            ResponseEntity.ok(tokens)
        } catch (ex: OAuthException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to (ex.message ?: "OAuth error")))
        }
    }

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

    private fun clientBinding(request: HttpServletRequest): String {
        val headerBinding = request.getHeader("X-Tasklight-Client")?.trim()
        return when {
            !headerBinding.isNullOrBlank() -> headerBinding
            else -> request.remoteAddr ?: "unknown"
        }
    }
}
