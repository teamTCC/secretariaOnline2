package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.time.Duration
import java.util.Date
import java.util.UUID

/**
 * Port for session management and access-token revocation backed by Redis.
 *
 * ## Session-based revocation (primary — sid in JWT)
 *
 * Every access token carries a `sid` (session ID) claim that maps to a short-lived
 * Redis key (`auth:session:<sid>`). The filter checks this key on every request.
 * Deleting the key immediately invalidates any access token that carries this `sid`,
 * regardless of JWT expiry — enabling true instantaneous logout.
 *
 * - [createSession]: call on login and on every refresh (generates fresh `sid`).
 * - [deleteSession]: call on logout (fail-open: cookies are cleared anyway; session
 *   expires naturally via TTL if Redis is temporarily unavailable).
 * - [sessionExists]: called by [JwtAuthenticationFilter] — **fail-closed**: throws when
 *   Redis is unavailable so that the filter does NOT authenticate the request.
 *
 * ## Force-logout (secondary — user-level marker)
 *
 * Used when refresh-token reuse is detected (possible token theft) or after a password
 * reset. Marks the user's user ID with the current timestamp; any access token issued
 * before that timestamp is rejected, covering tokens that were issued before the attack
 * was detected and are not yet expired.
 *
 * ## Legacy JTI blacklist (tertiary — backward compat)
 *
 * Used for graceful rollout when tokens without a `sid` claim are still in circulation.
 * Also covers edge cases where a session cannot be resolved.
 */
interface TokenRevocationPort {
    // ── Session (primary) ────────────────────────────────────────────────────

    /**
     * Creates a Redis session entry for the given [sid].
     * TTL should be access-token TTL + a small clock-skew buffer (e.g. +60 s).
     * Throws if Redis is unavailable — login must fail if no session can be recorded.
     */
    fun createSession(sid: String, userId: UUID, ttl: Duration)

    /**
     * Deletes the Redis session entry for [sid] (instant logout).
     * Fail-open: logs a warning if Redis is unavailable; the session expires
     * naturally via its TTL (≤ access-token TTL).
     */
    fun deleteSession(sid: String)

    /**
     * Returns `true` if a Redis session for [sid] exists and has not expired.
     * **Fail-closed**: throws when Redis is unavailable so the caller (filter)
     * can treat the request as unauthenticated rather than silently bypassing revocation.
     */
    fun sessionExists(sid: String): Boolean

    // ── Force-logout (secondary) ─────────────────────────────────────────────

    /**
     * Sets a force-logout marker for [userId] with TTL = [ttl].
     * Any access token whose `iat` precedes the stored timestamp is rejected.
     * Fail-open: logs a warning if Redis is unavailable.
     */
    fun forceLogoutUser(userId: UUID, ttl: Duration)

    /**
     * Returns `true` when a force-logout marker for [userId] is newer than [tokenIssuedAt].
     * Fail-closed: throws when Redis is unavailable.
     */
    fun isUserForcedLogout(userId: UUID, tokenIssuedAt: Date): Boolean

    // ── JTI blacklist (legacy / backward compat) ─────────────────────────────

    /**
     * Blacklists a specific access token by its JTI.
     * Used during graceful rollout (tokens without `sid` still in circulation)
     * and for one-time tokens. Fail-open.
     */
    fun revokeAccessToken(jti: String, expiresAt: Date)

    /**
     * Returns `true` if the JTI has been individually revoked.
     * Fail-closed: throws when Redis is unavailable.
     */
    fun isRevoked(jti: String): Boolean
}
