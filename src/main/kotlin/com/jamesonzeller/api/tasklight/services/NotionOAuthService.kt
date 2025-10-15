package com.jamesonzeller.api.tasklight.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AUTH_URL = "https://api.notion.com/v1/oauth/authorize"
private const val TOKEN_URL = "https://api.notion.com/v1/oauth/token"
private const val CODE_CHALLENGE_METHOD = "S256"
private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
private const val HMAC_ALGORITHM = "HmacSHA256"
private const val GCM_TAG_BITS = 128
private const val IV_LENGTH_BYTES = 12

@Service
class NotionOAuthService(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    @Value("\${NOTION_CLIENT_ID}") private val clientId: String,
    @Value("\${NOTION_CLIENT_SECRET}") private val clientSecret: String,
    @Value("\${TASKLIGHT_NOTION_REDIRECT_URI}") private val redirectUri: String,
    @Value("\${TASKLIGHT_NOTION_DEEP_LINK_URI}") private val deepLinkBaseUri: String,
    @Value("\${TASKLIGHT_NOTION_LOOPBACK_URI:}") private val loopbackBaseUri: String?,
    @Value("\${TASKLIGHT_OAUTH_STATE_KEY}") stateKey: String,
    @Value("\${TASKLIGHT_OAUTH_ENCRYPTION_KEY}") encryptionKey: String,
    @Value("\${TASKLIGHT_OAUTH_HANDOFF_TTL_SECONDS:240}") private val handoffTtlSeconds: Long
) {
    private val logger = LoggerFactory.getLogger(NotionOAuthService::class.java)
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private val secureRandom = SecureRandom()
    private val stateSigner = StateSigner(stateKey)
    private val cipher = TokenCipher(encryptionKey)

    fun startAuthorization(clientBinding: String?): AuthorizationStart {
        val handoffId = generateId()
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = stateSigner.sign(handoffId, clientBinding)
        val now = Instant.now().epochSecond

        val record = NotionOAuthHandoff(
            handoffId = handoffId,
            state = state,
            codeVerifier = codeVerifier,
            createdAtEpochSeconds = now,
            status = HandoffStatus.PENDING,
            encryptedTokens = null,
            clientBinding = clientBinding
        )

        save(record, Duration.ofSeconds(handoffTtlSeconds))

        val redirect = buildAuthorizationUri(state, codeChallenge)
        return AuthorizationStart(redirect, handoffId)
    }

    fun handleCallback(code: String, state: String): CallbackResult {
        val parsed = parseState(state)
        val record = load(parsed.handoffId)
            ?: throw OAuthException("Unknown or expired handoff")

        if (!stateSigner.verify(parsed, record)) {
            throw OAuthException("State verification failed")
        }
        if (record.status != HandoffStatus.PENDING) {
            throw OAuthException("Handoff already fulfilled")
        }

        val tokenPayload = exchangeCodeForTokens(code, record.codeVerifier)
        val encrypted = cipher.encrypt(tokenPayload.toByteArray(StandardCharsets.UTF_8))
        val updated = record.copy(
            status = HandoffStatus.READY,
            encryptedTokens = encrypted
        )
        save(updated, Duration.ofSeconds(handoffTtlSeconds))

        val deepLink = withHandoff(deepLinkBaseUri, record.handoffId)
        val loopback = loopbackBaseUri?.takeIf { it.isNotBlank() }?.let { withHandoff(it, record.handoffId) }
        val html = buildCallbackPage(deepLink, loopback)

        return CallbackResult(record.handoffId, html)
    }

    fun completeHandoff(handoffId: String, clientBinding: String?): JsonNode {
        val record = load(handoffId) ?: throw OAuthException("Unknown or expired handoff")
        if (record.status != HandoffStatus.READY || record.encryptedTokens == null) {
            throw OAuthException("Handoff not ready")
        }
        if (!stateSigner.matchesBinding(record, clientBinding)) {
            throw OAuthException("Client binding mismatch")
        }

        val decrypted = cipher.decrypt(record.encryptedTokens)
        delete(handoffId)
        return mapper.readTree(decrypted)
    }

    private fun exchangeCodeForTokens(code: String, codeVerifier: String): String {
        val body: ObjectNode = mapper.createObjectNode().apply {
            put("grant_type", "authorization_code")
            put("code", code)
            put("redirect_uri", redirectUri)
            put("code_verifier", codeVerifier)
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", basicAuthHeader())
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            logger.warn("Notion token exchange failed with status {}", response.statusCode())
            throw OAuthException("Failed to exchange authorization code")
        }

        return response.body()
    }

    private fun buildAuthorizationUri(state: String, codeChallenge: String): URI {
        val query = mapOf(
            "client_id" to clientId,
            "response_type" to "code",
            "owner" to "user",
            "redirect_uri" to redirectUri,
            "state" to state,
            "code_challenge" to codeChallenge,
            "code_challenge_method" to CODE_CHALLENGE_METHOD
        ).entries.joinToString("&") { (key, value) ->
            "${key}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
        return URI.create("$AUTH_URL?$query")
    }

    private fun save(record: NotionOAuthHandoff, ttl: Duration) {
        val json = mapper.writeValueAsString(record)
        redis.opsForValue().set(handoffKey(record.handoffId), json, ttl)
    }

    private fun delete(handoffId: String) {
        redis.delete(handoffKey(handoffId))
    }

    private fun load(handoffId: String): NotionOAuthHandoff? {
        val value = redis.opsForValue().get(handoffKey(handoffId)) ?: return null
        return mapper.readValue(value, NotionOAuthHandoff::class.java)
    }

    private fun generateId(): String = randomBytes(32)

    private fun generateCodeVerifier(): String = randomBytes(64)

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed)
    }

    private fun randomBytes(byteCount: Int): String {
        val buffer = ByteArray(byteCount)
        secureRandom.nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }

    private fun withHandoff(base: String, handoffId: String): String {
        val connector = if (base.contains("?")) "&" else "?"
        val encoded = URLEncoder.encode(handoffId, StandardCharsets.UTF_8)
        return "$base${connector}handoff=$encoded"
    }

    private fun buildCallbackPage(deepLink: String, loopback: String?): String {
        val deepLinkJson = mapper.writeValueAsString(deepLink)
        val loopbackJson = loopback?.let { mapper.writeValueAsString(it) }
        val linkLabel = "Return to Tasklight"

        val fallbackDirective = loopbackJson?.let {
            """
            setTimeout(function() {
                window.location.href = $it;
            }, 1500);
            """.trimIndent()
        } ?: """
            setTimeout(function() {
                window.close();
            }, 1500);
        """.trimIndent()

        val fallbackAnchor = loopback?.let {
            "<a href=\"$it\">$linkLabel</a>"
        } ?: "<p>You can close this tab.</p>"

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8" />
                <title>Tasklight</title>
                <meta http-equiv="refresh" content="0;url=$deepLink" />
            </head>
            <body>
                <p>Redirecting back to Tasklight…</p>
                $fallbackAnchor
                <script>
                    const target = $deepLinkJson;
                    try {
                        window.location.replace(target);
                    } catch (err) {
                        console.error("Failed to redirect to app", err);
                    }
                    $fallbackDirective
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun basicAuthHeader(): String {
        val raw = "$clientId:$clientSecret"
        val encoded = Base64.getEncoder().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encoded"
    }

    private fun handoffKey(handoffId: String): String = "tasklight:notion:handoff:$handoffId"
}

data class AuthorizationStart(
    val redirectUri: URI,
    val handoffId: String
)

data class CallbackResult(
    val handoffId: String,
    val html: String
)

class OAuthException(message: String) : RuntimeException(message)

data class NotionOAuthHandoff(
    val handoffId: String,
    val state: String,
    val codeVerifier: String,
    val createdAtEpochSeconds: Long,
    val status: HandoffStatus,
    val encryptedTokens: String?,
    val clientBinding: String?
)

enum class HandoffStatus {
    PENDING,
    READY
}

private class ParsedState(val handoffId: String, val signature: String)

private class StateSigner(secret: String) {
    private val keySpec: SecretKeySpec = SecretKeySpec(decode(secret), HMAC_ALGORITHM)

    fun sign(handoffId: String, binding: String?): String {
        val signature = signInternal(composePayload(handoffId, binding))
        return "$handoffId.$signature"
    }

    fun verify(parsed: ParsedState, record: NotionOAuthHandoff): Boolean {
        val expected = signInternal(composePayload(parsed.handoffId, record.clientBinding))
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.US_ASCII),
            parsed.signature.toByteArray(StandardCharsets.US_ASCII)
        )
    }

    fun matchesBinding(record: NotionOAuthHandoff, binding: String?): Boolean {
        val expected = composePayload(record.handoffId, record.clientBinding)
        val actual = composePayload(record.handoffId, binding)
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            actual.toByteArray(StandardCharsets.UTF_8)
        )
    }

    private fun signInternal(payload: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(keySpec)
        val raw = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
    }

    private fun composePayload(handoffId: String, binding: String?): String {
        return "$handoffId:${binding ?: "anon"}"
    }

    companion object {
        fun parse(state: String): ParsedState {
            val parts = state.split('.', limit = 2)
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw OAuthException("Malformed state parameter")
            }
            return ParsedState(parts[0], parts[1])
        }

        private fun decode(secret: String): ByteArray {
            return try {
                Base64.getDecoder().decode(secret)
            } catch (ex: IllegalArgumentException) {
                secret.toByteArray(StandardCharsets.UTF_8)
            }
        }
    }
}

private fun parseState(state: String): ParsedState = StateSigner.parse(state)

private class TokenCipher(secret: String) {
    private val keySpec: SecretKeySpec
    private val secureRandom = SecureRandom()

    init {
        val keyBytes = deriveKey(secret)
        keySpec = SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plain: ByteArray): String {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plain)
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.getUrlEncoder().encodeToString(combined)
    }

    fun decrypt(encrypted: String): ByteArray {
        val combined = Base64.getUrlDecoder().decode(encrypted)
        if (combined.size <= IV_LENGTH_BYTES) {
            throw OAuthException("Corrupted payload")
        }
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(secret: String): ByteArray {
        val raw = try {
            Base64.getDecoder().decode(secret)
        } catch (ex: IllegalArgumentException) {
            secret.toByteArray(StandardCharsets.UTF_8)
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(raw)
    }
}
