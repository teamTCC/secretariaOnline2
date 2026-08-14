package br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(
    name = "notification_log",
    indexes = [Index(name = "idx_notification_log_aggregate", columnList = "aggregate_id")],
)
class NotificationLogEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,
    @Column(name = "id_usuario")
    val idUsuario: UUID? = null,
    @Column(nullable = false, length = 20)
    val canal: String = "EMAIL",
    @Column(nullable = false, length = 20)
    val status: String = "SENT",
) : BaseEntity(id)

interface NotificationLogJpaRepository : JpaRepository<NotificationLogEntity, UUID>
