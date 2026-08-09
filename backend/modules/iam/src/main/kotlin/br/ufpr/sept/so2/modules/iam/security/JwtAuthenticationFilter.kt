package br.ufpr.sept.so2.modules.iam.security

import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.shared.security.AuthenticatedUser
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        extractToken(request)?.let { token ->
            try {
                val claims = jwtTokenService.verify(token)
                val userId = UUID.fromString(claims.payload.subject)

                @Suppress("UNCHECKED_CAST")
                val authorities = (claims.payload["authorities"] as? List<String>)?.toSet() ?: emptySet()

                val principal = AuthenticatedUser(userId = userId, authorities = authorities)
                val grantedAuthorities = authorities.map { SimpleGrantedAuthority(it) }

                val authentication = UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (ex: JwtException) {
                log.debug("JWT inválido: {}", ex.message)
            } catch (ex: Exception) {
                log.warn("Erro ao processar JWT: {}", ex.message)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerPrefix = "Bearer "
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith(bearerPrefix)) header.removePrefix(bearerPrefix).trim() else null
    }
}
