package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "password_history",
    indexes = [
        Index(name = "idx_pwd_history_usuario", columnList = "id_usuario"),
    ],
)
class PasswordHistoryEntity(
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "id_usuario", nullable = false)
    val usuarioId: UUID,
    @Column(name = "senha_hash", nullable = false, length = 300)
    val senhaHash: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
