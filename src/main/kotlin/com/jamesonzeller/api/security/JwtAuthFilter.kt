package com.jamesonzeller.api.security

import com.jamesonzeller.api.tasklight.services.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
): GenericFilterBean() {
    override fun doFilter(request: ServletRequest, response: ServletResponse, filterChain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val authHeader = httpRequest.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer").trim()
            val userId = jwtService.validateToken(token)

            if (userId != null) {
                httpRequest.setAttribute("userId", userId.toString())
            } else {
                (response as HttpServletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header")
                return
            }

            filterChain.doFilter(request, response)
        }
    }
}