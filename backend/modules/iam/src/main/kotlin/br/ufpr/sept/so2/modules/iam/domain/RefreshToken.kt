package br.ufpr.sept.so2.modules.iam.domain

import java.time.OffsetDateTime
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val value: String,
    val usuarioId: UUID,
    val expiresAt: OffsetDateTime,
    val usedAt: OffsetDateTime?,
    val revokedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
) {
    fun isExpired(): Boolean = expiresAt.isBefore(OffsetDateTime.now())

    fun isUsed(): Boolean = usedAt != null

    fun isRevoked(): Boolean = revokedAt != null

    fun isValid(): Boolean = !isExpired() && !isUsed() && !isRevoked()

    companion object {
        private val TOKEN_TTL_DAYS = 7L

        fun issue(usuarioId: UUID): RefreshToken =
            RefreshToken(
                id = UUID.randomUUID(),
                value = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""),
                usuarioId = usuarioId,
                expiresAt = OffsetDateTime.now().plusDays(TOKEN_TTL_DAYS),
                usedAt = null,
                revokedAt = null,
                createdAt = OffsetDateTime.now(),
            )
    }
}
