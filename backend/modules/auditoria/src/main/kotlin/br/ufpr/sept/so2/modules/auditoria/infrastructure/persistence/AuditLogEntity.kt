package br.ufpr.sept.so2.modules.auditoria.infrastructure.persistence

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "audit_log",
    indexes = [
        Index(name = "idx_audit_log_ator", columnList = "id_ator"),
        Index(name = "idx_audit_log_acao", columnList = "acao"),
        Index(name = "idx_audit_log_at", columnList = "at"),
        Index(name = "idx_audit_log_alvo", columnList = "alvo_tipo, alvo_id"),
    ],
)
class AuditLogEntity(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "at", nullable = false, updatable = false)
    val at: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "id_ator")
    val idAtor: UUID?,
    @Column(nullable = false, length = 100)
    val acao: String,
    @Column(name = "alvo_tipo", length = 60)
    val alvoTipo: String?,
    @Column(name = "alvo_id")
    val alvoId: UUID?,
    @Column(length = 45)
    val ip: String?,
    @Column(name = "user_agent", columnDefinition = "text")
    val userAgent: String?,
    @Column(nullable = false, length = 20)
    val resultado: String,
    @Column(nullable = false, columnDefinition = "jsonb")
    @Type(JsonType::class)
    val payload: Map<String, Any?> = emptyMap(),
)
