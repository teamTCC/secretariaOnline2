package br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence

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
    name = "communication",
    indexes = [
        Index(name = "idx_communication_autor", columnList = "id_autor"),
        Index(name = "idx_communication_published_at", columnList = "published_at"),
    ],
)
class CommunicationEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_autor", nullable = false)
    val idAutor: UUID,
    @Column(nullable = false, length = 200)
    var titulo: String,
    @Column(nullable = false, columnDefinition = "text")
    var conteudo: String,
    @Column(nullable = false, length = 20)
    var tipo: String,
    @Column(nullable = false, columnDefinition = "jsonb")
    @Type(JsonType::class)
    var audiencia: Map<String, Any> = emptyMap(),
    @Column(name = "published_at")
    var publishedAt: OffsetDateTime? = null,
    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null,
) : BaseEntity(id)

@Entity
@Table(
    name = "communication_delivery",
    indexes = [
        Index(name = "idx_comm_delivery_communication", columnList = "id_communication"),
        Index(name = "idx_comm_delivery_usuario", columnList = "id_usuario"),
    ],
)
class CommunicationDeliveryEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_communication", nullable = false)
    val idCommunication: UUID,
    @Column(name = "id_usuario", nullable = false)
    val idUsuario: UUID,
    @Column(nullable = false, length = 20)
    var canal: String,
    @Column(nullable = false, length = 20)
    var status: String = "PENDENTE",
    @Column(name = "delivered_at")
    var deliveredAt: OffsetDateTime? = null,
    @Column(name = "read_at")
    var readAt: OffsetDateTime? = null,
) : BaseEntity(id)

@Entity
@Table(
    name = "notification_preference",
    indexes = [Index(name = "idx_notif_pref_usuario", columnList = "id_usuario", unique = true)],
)
class NotificationPreferenceEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_usuario", nullable = false, unique = true)
    val idUsuario: UUID,
    @Column(name = "email_enabled", nullable = false)
    var emailEnabled: Boolean = true,
    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = true,
    @Column(name = "in_app_enabled", nullable = false)
    var inAppEnabled: Boolean = true,
) : BaseEntity(id)
