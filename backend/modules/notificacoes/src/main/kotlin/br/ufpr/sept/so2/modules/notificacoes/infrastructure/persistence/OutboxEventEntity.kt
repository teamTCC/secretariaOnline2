package br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "outbox_event",
    indexes = [
        Index(name = "idx_outbox_status", columnList = "status"),
        Index(name = "idx_outbox_next_attempt", columnList = "next_attempt_at"),
    ],
)
class OutboxEventEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,
    @Column(name = "aggregate_type", nullable = false, length = 60)
    val aggregateType: String,
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,
    @Column(nullable = false, columnDefinition = "jsonb")
    @Type(JsonType::class)
    val payload: Map<String, Any> = emptyMap(),
    @Column(nullable = false, length = 20)
    var status: String = "PENDING",
    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,
    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "processed_at")
    var processedAt: OffsetDateTime? = null,
    @Column(name = "last_error", columnDefinition = "text")
    var lastError: String? = null,
) : BaseEntity(id)
