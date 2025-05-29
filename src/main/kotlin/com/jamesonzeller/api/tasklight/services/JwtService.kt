package com.jamesonzeller.api.tasklight.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("JWT_SECRET") private val jwtSecretBase64: String,
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(
        jwtSecretBase64.toByteArray(),
    )

    private val issuer= "tasklight"
    private val expirationMs = 10L * 365 * 24 * 60 * 60 * 1000

    fun generateToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): UUID? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body

            UUID.fromString(claims.subject)
        } catch (e: Exception) {
            null
        }
    }
}