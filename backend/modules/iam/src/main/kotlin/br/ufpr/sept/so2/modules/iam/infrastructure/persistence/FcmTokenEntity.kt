package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "device_fcm_token",
    uniqueConstraints = [UniqueConstraint(columnNames = ["id_usuario", "fcm_token"])],
    indexes = [Index(name = "idx_fcm_token_usuario", columnList = "id_usuario")],
)
class FcmTokenEntity(
    id: UUID = UUID.randomUUID(),
    @Column(name = "id_usuario", nullable = false)
    val idUsuario: UUID,
    @Column(name = "fcm_token", nullable = false, length = 500)
    var fcmToken: String,
    @Column(nullable = false, length = 20)
    var plataforma: String = "ANDROID",
    @Column(nullable = false)
    var ativo: Boolean = true,
) : BaseEntity(id)
