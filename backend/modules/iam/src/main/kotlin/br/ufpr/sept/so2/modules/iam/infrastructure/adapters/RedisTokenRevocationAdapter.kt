package br.ufpr.sept.so2.modules.iam.infrastructure.adapters

import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenRevocationPort
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.Date
import java.util.UUID

/**
 * Redis-backed implementation of [TokenRevocationPort].
 *
 * Key schema:
 *  - `auth:revoked:jti:<jti>`        → "1"       TTL = remaining token lifetime
 *  - `auth:force-logout:user:<uuid>` → epoch-ms  TTL = access-token TTL
 *
 * Fail-open policy: if Redis is unavailable, a warning is logged and the operation
 * is skipped rather than crashing the application.
 */
@Repository
class RedisTokenRevocationAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
) : TokenRevocationPort {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val JTI_PREFIX = "auth:revoked:jti:"
        private const val FORCE_LOGOUT_PREFIX = "auth:force-logout:user:"
    }

    override fun revokeAccessToken(
        jti: String,
        expiresAt: Date,
    ) {
        val ttlMillis = expiresAt.time - System.currentTimeMillis()
        if (ttlMillis <= 0) return
        runCatching {
            redisTemplate.opsForValue().set("$JTI_PREFIX$jti", "1", Duration.ofMillis(ttlMillis))
        }.onFailure { log.warn("Redis unavailable — could not blacklist JTI {}: {}", jti, it.message) }
    }

    override fun forceLogoutUser(
        userId: UUID,
        ttl: Duration,
    ) {
        runCatching {
            redisTemplate.opsForValue().set(
                "$FORCE_LOGOUT_PREFIX$userId",
                System.currentTimeMillis().toString(),
                ttl,
            )
        }.onFailure { log.warn("Redis unavailable — could not set force-logout for user {}: {}", userId, it.message) }
    }

    override fun isRevoked(jti: String): Boolean =
        runCatching {
            redisTemplate.hasKey("$JTI_PREFIX$jti") == true
        }.getOrElse { ex ->
            log.warn("Redis unavailable — skipping JTI blacklist check for {}: {}", jti, ex.message)
            false
        }

    override fun isUserForcedLogout(
        userId: UUID,
        tokenIssuedAt: Date,
    ): Boolean =
        runCatching {
            val forceLogoutAtStr = redisTemplate.opsForValue().get("$FORCE_LOGOUT_PREFIX$userId")
                ?: return false
            val forceLogoutAt = forceLogoutAtStr.toLongOrNull() ?: return false
            tokenIssuedAt.time < forceLogoutAt
        }.getOrElse { ex ->
            log.warn("Redis unavailable — skipping force-logout check for user {}: {}", userId, ex.message)
            false
        }
}
