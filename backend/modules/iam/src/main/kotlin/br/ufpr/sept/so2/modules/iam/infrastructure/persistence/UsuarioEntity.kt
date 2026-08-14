package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.Type
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "usuario",
    indexes = [
        Index(name = "idx_usuario_email", columnList = "email", unique = true),
        Index(name = "idx_usuario_grr", columnList = "grr", unique = true),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class UsuarioEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 200)
    var nome: String,
    @Column(columnDefinition = "citext", unique = true, nullable = false, length = 200)
    var email: String,
    @Column(nullable = true, length = 20, unique = true)
    var grr: String? = null,
    @Column(name = "senha_hash", nullable = false, length = 300)
    var senhaHash: String,
    @Column(name = "senha_alterada", nullable = false)
    var senhaAlterada: Boolean = false,
    @Column(nullable = false)
    var ativo: Boolean = true,
    @Column(name = "bloqueado_ate")
    var bloqueadoAte: OffsetDateTime? = null,
    @Column(name = "tentativas_falhas", nullable = false)
    var tentativasFalhas: Int = 0,
    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(JsonType::class)
    var metadata: MutableMap<String, Any> = mutableMapOf(),
    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
    @OneToMany(mappedBy = "usuario", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val usuarioRoles: MutableList<UsuarioRoleEntity> = mutableListOf(),
) : BaseEntity(id)
