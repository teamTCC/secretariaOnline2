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
 * ## Revocation check order (fail-closed)
 *
 * 1. **Session check** (`sid` claim present): verifies `auth:session:<sid>` exists in Redis.
 *    If the key is missing the token is rejected. If Redis is down the exception propagates,
 *    leaving the authentication context empty — the request is effectively unauthenticated.
 *
 * 2. **Force-logout check** (always): rejects tokens issued before the user's force-logout
 *    marker (covers scenarios where `sid`-based revocation is bypassed, e.g. tokens without
 *    `sid` still in circulation during a rolling deployment).
 *
 * 3. **JTI blacklist** (`sid` absent — legacy / backward compat): direct JTI lookup in Redis.
 *
 * All three checks are fail-closed: a Redis outage is treated as "revoked" rather than
 * "allowed", because availability is a lesser concern than passing a revoked token.
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
                val sid = payload["sid"] as? String

                // 1. Session check (primary — fail-closed)
                if (sid != null) {
                    if (!tokenRevocationPort.sessionExists(sid)) {
                        log.debug("Session not found — rejecting token for userId={} sid={}", userId, sid)
                        filterChain.doFilter(request, response)
                        return
                    }
                } else {
                    // 3. JTI blacklist (legacy — no sid claim)
                    if (jti != null && tokenRevocationPort.isRevoked(jti)) {
                        log.debug("Access token JTI {} is blacklisted — rejecting", jti)
                        filterChain.doFilter(request, response)
                        return
                    }
                }

                // 2. Force-logout check (always, regardless of sid)
                if (issuedAt != null && tokenRevocationPort.isUserForcedLogout(userId, issuedAt)) {
                    log.debug("User {} force-logged-out — token issued before force-logout event", userId)
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
                log.warn("Erro ao processar JWT (Redis indisponível?): {}", ex.message)
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
