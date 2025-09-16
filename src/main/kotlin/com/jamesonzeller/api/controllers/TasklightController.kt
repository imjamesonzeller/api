package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.tasklight.models.UsageIncrementResult
import com.jamesonzeller.api.tasklight.models.UsageStatus
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

const val DAILY_LIMIT = 3

@RestController
@RequestMapping("/tasklight")
class TasklightController(
    private val redis: StringRedisTemplate
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

    @GetMapping("/check_usage")
    fun checkUsage(
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val userId = notionUserId(request)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Missing X-Notion-User-Id header"))

        val today = LocalDate.now(zone)
        val used = getUsed(userId)
        val remaining = (DAILY_LIMIT - used).coerceAtLeast(0)
        val allowed = used < DAILY_LIMIT

        return ResponseEntity.ok(
            UsageStatus(
                allowed = allowed,
                used = used,
                remaining = remaining.toLong(),
                limit = DAILY_LIMIT,
                date = today.toString()
            )
        )
    }

    @PostMapping("/increment_usage")
    fun incrementUsage(
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val userId = notionUserId(request)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Missing X-Notion-User-Id header"))

        val today = LocalDate.now(zone)
        val key = dailyKey(userId, today)
        val newVal = redis.opsForValue().increment(key)!!

        setExpiryAtEndOfDayIfFirst(key, newVal)

        return ResponseEntity.ok(
            UsageIncrementResult(
                used = newVal,
                limit = DAILY_LIMIT,
                date = today.toString()
            )
        )
    }
}