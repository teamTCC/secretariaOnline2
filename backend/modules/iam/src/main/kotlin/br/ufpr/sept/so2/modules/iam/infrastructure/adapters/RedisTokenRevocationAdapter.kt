package br.ufpr.sept.so2.modules.iam.infrastructure.adapters

import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenRevocationPort
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.Date
import java.util.UUID

/**
 * Redis-backed implementation of [TokenRevocationPort].
 *
 * ## Key schema
 * | Key                              | Value     | TTL                    | Use              |
 * |----------------------------------|-----------|------------------------|------------------|
 * | `auth:session:<sid>`             | userId    | accessTTL + 60 s       | Session check    |
 * | `auth:force-logout:user:<uuid>`  | epoch-ms  | accessTTL              | User force-out   |
 * | `auth:revoked:jti:<jti>`         | "1"       | remaining token life   | Legacy / one-off |
 *
 * ## Fail policies
 * - `sessionExists`, `isRevoked`, `isUserForcedLogout`: **fail-closed** — exceptions
 *   propagate to [JwtAuthenticationFilter], which leaves the request unauthenticated.
 * - `createSession`: **fail-closed** — login must not succeed without a recorded session.
 * - `deleteSession`, `revokeAccessToken`, `forceLogoutUser`: **fail-open** — best-effort;
 *   entries expire via TTL when Redis is temporarily unavailable.
 */
@Repository
class RedisTokenRevocationAdapter(
    private val redisTemplate: StringRedisTemplate,
) : TokenRevocationPort {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SESSION_PREFIX = "auth:session:"
        private const val FORCE_LOGOUT_PREFIX = "auth:force-logout:user:"
        private const val JTI_PREFIX = "auth:revoked:jti:"
    }

    // ── Session (primary) ────────────────────────────────────────────────────

    override fun createSession(sid: String, userId: UUID, ttl: Duration) {
        redisTemplate.opsForValue().set("$SESSION_PREFIX$sid", userId.toString(), ttl)
        log.debug("Session created: sid={} userId={} ttl={}", sid, userId, ttl)
    }

    override fun deleteSession(sid: String) {
        runCatching {
            redisTemplate.delete("$SESSION_PREFIX$sid")
            log.debug("Session deleted: sid={}", sid)
        }.onFailure {
            log.warn("Redis unavailable — could not delete session sid={}: {}", sid, it.message)
        }
    }

    override fun sessionExists(sid: String): Boolean {
        // No runCatching — exceptions propagate so the filter treats the request as unauthenticated.
        val exists = redisTemplate.hasKey("$SESSION_PREFIX$sid") == true
        if (!exists) log.debug("Session not found in Redis: sid={}", sid)
        return exists
    }

    // ── Force-logout (secondary) ─────────────────────────────────────────────

    override fun forceLogoutUser(userId: UUID, ttl: Duration) {
        runCatching {
            redisTemplate.opsForValue().set(
                "$FORCE_LOGOUT_PREFIX$userId",
                System.currentTimeMillis().toString(),
                ttl,
            )
            log.info("Force-logout marker set for userId={} ttl={}", userId, ttl)
        }.onFailure {
            log.warn("Redis unavailable — could not set force-logout for userId={}: {}", userId, it.message)
        }
    }

    override fun isUserForcedLogout(userId: UUID, tokenIssuedAt: Date): Boolean {
        // No runCatching — fail-closed: exceptions propagate to filter.
        val markerStr = redisTemplate.opsForValue().get("$FORCE_LOGOUT_PREFIX$userId")
            ?: return false
        val forceLogoutAt = markerStr.toLongOrNull() ?: return false
        return tokenIssuedAt.time < forceLogoutAt
    }

    // ── JTI blacklist (legacy / backward compat) ─────────────────────────────

    override fun revokeAccessToken(jti: String, expiresAt: Date) {
        val ttlMillis = expiresAt.time - System.currentTimeMillis()
        if (ttlMillis <= 0) return
        runCatching {
            redisTemplate.opsForValue().set("$JTI_PREFIX$jti", "1", Duration.ofMillis(ttlMillis))
        }.onFailure {
            log.warn("Redis unavailable — could not blacklist JTI={}: {}", jti, it.message)
        }
    }

    override fun isRevoked(jti: String): Boolean {
        // No runCatching — fail-closed.
        return redisTemplate.hasKey("$JTI_PREFIX$jti") == true
    }
}
