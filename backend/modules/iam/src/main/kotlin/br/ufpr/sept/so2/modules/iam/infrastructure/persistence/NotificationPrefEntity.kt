package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

@Entity
@Table(
    name = "notification_preference",
    indexes = [Index(name = "idx_notif_pref_usuario", columnList = "id_usuario", unique = true)],
)
class NotificationPrefEntity(
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

interface NotificationPrefJpaRepository : JpaRepository<NotificationPrefEntity, UUID> {
    fun findByIdUsuario(idUsuario: UUID): Optional<NotificationPrefEntity>
}
