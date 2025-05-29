import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyFilter(private val expectedApiKey: String) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (path.startsWith("/mirror/")) {
            val apiKey = request.getHeader("x-api-key")

            if (apiKey.isNullOrEmpty() || apiKey != expectedApiKey) {
                println("❌ Invalid or missing API key")
                response.status = HttpServletResponse.SC_FORBIDDEN
                response.writer.write("Forbidden: Invalid API Key")
                return
            }


            val auth = UsernamePasswordAuthenticationToken(
                "api-key-user", null, listOf(SimpleGrantedAuthority("ROLE_API"))
            )
            println("✅ API key accepted, setting ROLE_API authentication")
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}