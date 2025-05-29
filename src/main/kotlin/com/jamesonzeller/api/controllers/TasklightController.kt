package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.tasklight.services.JwtService
import com.jamesonzeller.api.tasklight.services.UsageService
import com.jamesonzeller.api.tasklight.services.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/tasklight")
class TasklightController(
    private val userService: UserService,
    private val jwtService: JwtService,
    private val usageService: UsageService
) {
    data class AuthRequest(val email: String, val password: String)

    @PostMapping("/auth/register")
    fun register(
        @RequestBody req: AuthRequest
    ) : ResponseEntity<Any> {
        return try {
            val user = userService.register(req.email, req.password)
            val token = jwtService.generateToken(user.id!!)
            ResponseEntity.ok(mapOf("token" to token))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/auth/login")
    fun login(
        @RequestBody req: AuthRequest
    ) : ResponseEntity<Any> {
        return try {
            val user = userService.login(req.email, req.password)
            val token = jwtService.generateToken(user.id!!)
            ResponseEntity.ok(mapOf("token" to token))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/check_usage")
    fun checkUsage(
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val userId = request.getAttribute("userId")?.toString()?.let(UUID::fromString)
            ?: return ResponseEntity.status(401).build()

        val usage = usageService.getTodayUsage(userId)
        return ResponseEntity.ok(mapOf("requestsMade" to usage))
    }

    @PostMapping("/increment_usage")
    fun incrementUsage(
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val userId = request.getAttribute("userId")?.toString()?.let(UUID::fromString)
            ?: return ResponseEntity.status(401).build()

        val usage = usageService.incrementUsage(userId)
        return ResponseEntity.ok(mapOf("requestsMade" to usage))
    }
}