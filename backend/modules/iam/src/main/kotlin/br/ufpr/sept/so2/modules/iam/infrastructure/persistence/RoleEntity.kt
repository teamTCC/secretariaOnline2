package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.shared.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "role",
    indexes = [Index(name = "idx_role_code", columnList = "code", unique = true)],
)
class RoleEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 50)
    val code: String,
    @Column(nullable = false, length = 200)
    val descricao: String,
    @ManyToMany(fetch = FetchType.EAGER)
    @jakarta.persistence.JoinTable(
        name = "role_authority",
        joinColumns = [jakarta.persistence.JoinColumn(name = "id_role")],
        inverseJoinColumns = [jakarta.persistence.JoinColumn(name = "id_authority")],
    )
    val authorities: MutableSet<AuthorityEntity> = mutableSetOf(),
) : BaseEntity(id)

@Entity
@Table(
    name = "authority",
    indexes = [Index(name = "idx_authority_code", columnList = "code", unique = true)],
)
class AuthorityEntity(
    id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 100)
    val code: String,
    @Column(nullable = false, length = 200)
    val descricao: String,
) : BaseEntity(id)

@Entity
@Table(name = "usuario_role")
class UsuarioRoleEntity(
    id: UUID = UUID.randomUUID(),
    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "id_usuario", nullable = false)
    val usuario: UsuarioEntity,
    @jakarta.persistence.ManyToOne(fetch = FetchType.EAGER)
    @jakarta.persistence.JoinColumn(name = "id_role", nullable = false)
    val role: RoleEntity,
    @Column(columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.json.JsonType::class)
    val escopo: Map<String, Any> = emptyMap(),
) : BaseEntity(id)
