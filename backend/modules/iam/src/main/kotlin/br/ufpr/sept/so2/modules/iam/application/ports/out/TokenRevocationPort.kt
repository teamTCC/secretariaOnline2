package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.time.Duration
import java.util.Date
import java.util.UUID

/**
 * Port for revoking access tokens before their natural expiry.
 *
 * Two revocation strategies are supported:
 *  1. JTI-based: blacklists a specific token by its JWT ID (individual logout).
 *  2. User-level force-logout: marks a timestamp so that any token issued before
 *     that moment is rejected (forced session termination — e.g., token-theft response).
 *
 * Implementations are expected to use a TTL-capable store (e.g. Redis) so entries
 * are cleaned up automatically when the underlying tokens expire naturally.
 */
interface TokenRevocationPort {
    /**
     * Blacklist a specific access token identified by its JTI.
     * The entry should expire at the same time as the token itself.
     */
    fun revokeAccessToken(
        jti: String,
        expiresAt: Date,
    )

    /**
     * Force-logout all tokens for a user by recording the current timestamp in the store.
     * Any access token whose `iat` (issued-at) is earlier than this timestamp will be rejected.
     * The marker should expire after [ttl] so the store doesn't hold stale entries.
     */
    fun forceLogoutUser(
        userId: UUID,
        ttl: Duration,
    )

    /** Returns true if the given JTI has been individually revoked. */
    fun isRevoked(jti: String): Boolean

    /**
     * Returns true if the user has a force-logout marker that is newer than [tokenIssuedAt],
     * meaning the token was issued before the forced-logout event and must be rejected.
     */
    fun isUserForcedLogout(
        userId: UUID,
        tokenIssuedAt: Date,
    ): Boolean
}
