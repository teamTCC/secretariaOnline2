package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.time.OffsetDateTime

/**
 * Postgres store for **email** one-time JWTs (password-reset links).
 * Distinct from Redis `auth:revoked:jti:<jti>` used for access-token / deep-link revocation.
 */
interface EmailOneTimeTokenStore {
    fun add(
        jti: String,
        expiresAt: OffsetDateTime,
    )

    fun exists(jti: String): Boolean

    fun deleteExpired()
}
