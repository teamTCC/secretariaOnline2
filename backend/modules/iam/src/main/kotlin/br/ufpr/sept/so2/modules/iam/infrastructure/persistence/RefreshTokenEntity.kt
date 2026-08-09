package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "refresh_token",
    indexes = [
        Index(name = "idx_refresh_token_value", columnList = "value", unique = true),
        Index(name = "idx_refresh_token_usuario", columnList = "id_usuario"),
    ],
)
class RefreshTokenEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 128)
    val value: String,
    @Column(name = "id_usuario", nullable = false)
    val usuarioId: UUID,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,
    @Column(name = "used_at")
    var usedAt: OffsetDateTime? = null,
    @Column(name = "revoked_at")
    var revokedAt: OffsetDateTime? = null,
) : BaseEntity(id)
