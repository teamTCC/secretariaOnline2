package br.ufpr.sept.so2.modules.iam.security

import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenRevocationPort
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

/**
 * Extracts the access token from:
 *  1. The `access_token` HttpOnly cookie (primary — browser flows)
 *  2. The `Authorization: Bearer <token>` header (fallback — API/Swagger/httpie)
 *
 * After signature verification the token is checked against the Redis revocation store
 * (individual JTI blacklist + per-user force-logout marker) before the authentication
 * is set in the SecurityContext.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
    private val tokenRevocationPort: TokenRevocationPort,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        extractToken(request)?.let { token ->
            try {
                val jws = jwtTokenService.verify(token)
                val payload = jws.payload

                val jti = payload.id
                val userId = UUID.fromString(payload.subject)
                val issuedAt = payload.issuedAt

                if (jti != null && tokenRevocationPort.isRevoked(jti)) {
                    log.debug("Access token JTI {} is blacklisted — rejecting", jti)
                    filterChain.doFilter(request, response)
                    return
                }

                if (issuedAt != null && tokenRevocationPort.isUserForcedLogout(userId, issuedAt)) {
                    log.debug("User {} has been force-logged-out — token issued before force-logout event", userId)
                    filterChain.doFilter(request, response)
                    return
                }

                @Suppress("UNCHECKED_CAST")
                val authorities = (payload["authorities"] as? List<String>)?.toSet() ?: emptySet()
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

    /**
     * Cookie takes priority over the Authorization header so that browser-based
     * flows work seamlessly. API/Swagger/httpie callers may still use Bearer.
     */
    private fun extractToken(request: HttpServletRequest): String? {
        request.cookies?.firstOrNull { it.name == "access_token" }?.value
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.removePrefix("Bearer ").trim() else null
    }
}
