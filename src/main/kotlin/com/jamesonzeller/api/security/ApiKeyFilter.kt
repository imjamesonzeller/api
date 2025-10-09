import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.filter.OncePerRequestFilter
import org.slf4j.LoggerFactory

class ApiKeyFilter(private val expectedApiKey: String) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(ApiKeyFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (path.startsWith("/mirror/")) {
            val apiKey = request.getHeader("x-api-key")

            if (apiKey.isNullOrEmpty() || apiKey != expectedApiKey) {
                log.warn("Invalid or missing API key for path {}", path)
                response.status = HttpServletResponse.SC_FORBIDDEN
                response.writer.write("Forbidden: Invalid API Key")
                return
            }


            val auth = UsernamePasswordAuthenticationToken(
                "api-key-user", null, listOf(SimpleGrantedAuthority("ROLE_API"))
            )
            log.info("API key accepted, setting ROLE_API authentication for path {}", path)
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
