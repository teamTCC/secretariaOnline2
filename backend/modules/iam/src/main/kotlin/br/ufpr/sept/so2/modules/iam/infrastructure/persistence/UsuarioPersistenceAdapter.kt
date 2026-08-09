package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.domain.Authority
import br.ufpr.sept.so2.modules.iam.domain.Role
import br.ufpr.sept.so2.modules.iam.domain.Usuario
import br.ufpr.sept.so2.modules.iam.domain.UsuarioRole
import br.ufpr.sept.so2.shared.domain.valueobject.Email
import br.ufpr.sept.so2.shared.domain.valueobject.Grr
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class UsuarioPersistenceAdapter(
    private val jpaRepository: UsuarioJpaRepository,
    private val entityManager: EntityManager,
) : UsuarioRepository {
    override fun findById(id: UUID): Usuario? = jpaRepository.findByIdWithRoles(id).map { it.toDomain() }.orElse(null)

    override fun findByIdentificador(identificador: String): Usuario? {
        val trimmed = identificador.trim()
        return if (trimmed.startsWith("GRR", ignoreCase = true)) {
            jpaRepository.findByGrrWithRoles(trimmed.uppercase()).map { it.toDomain() }.orElse(null)
        } else {
            jpaRepository.findByEmailWithRoles(trimmed.lowercase()).map { it.toDomain() }.orElse(null)
        }
    }

    override fun findByEmail(email: String): Usuario? =
        jpaRepository.findByEmailWithRoles(email.lowercase()).map { it.toDomain() }.orElse(null)

    override fun findByGrr(grr: String): Usuario? = jpaRepository.findByGrrWithRoles(grr.uppercase()).map { it.toDomain() }.orElse(null)

    override fun save(usuario: Usuario): Usuario {
        val entity = jpaRepository.findById(usuario.id).orElseGet { UsuarioEntity(id = usuario.id, nome = "", email = "", senhaHash = "") }
        entity.nome = usuario.nome
        entity.email = usuario.email.value
        entity.grr = usuario.grr?.value
        entity.senhaHash = usuario.senhaHash
        entity.senhaAlterada = usuario.senhaAlterada
        entity.ativo = usuario.ativo
        entity.bloqueadoAte = usuario.bloqueadoAte
        entity.tentativasFalhas = usuario.tentativasFalhas
        entity.metadata = usuario.metadata.toMutableMap()
        return jpaRepository.save(entity).toDomain()
    }

    override fun findAll(pageable: Pageable): Page<Usuario> = jpaRepository.findAll(pageable).map { it.toDomain() }

    override fun existsByEmail(email: String): Boolean = jpaRepository.existsByEmail(email.lowercase())

    override fun existsByGrr(grr: String): Boolean = jpaRepository.existsByGrr(grr.uppercase())

    override fun updateFailedAttempts(
        id: UUID,
        attempts: Int,
        bloqueadoAte: OffsetDateTime?,
    ) {
        jpaRepository.updateFailedAttempts(id, attempts, bloqueadoAte)
    }

    override fun updatePassword(
        id: UUID,
        newHash: String,
    ) {
        jpaRepository.updatePassword(id, newHash)
    }

    override fun updateMetadata(
        id: UUID,
        metadata: Map<String, Any>,
    ) {
        val entity = jpaRepository.findById(id).orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }
        entity.metadata = metadata.toMutableMap()
        jpaRepository.save(entity)
    }

    override fun invalidateAllSessions(id: UUID) {
        // Handled by RefreshTokenPersistenceAdapter
    }

    private fun UsuarioEntity.toDomain(): Usuario =
        Usuario(
            id = this.id,
            nome = this.nome,
            email = Email.of(this.email),
            grr = this.grr?.let { Grr.of(it) },
            senhaHash = this.senhaHash,
            senhaAlterada = this.senhaAlterada,
            ativo = this.ativo,
            bloqueadoAte = this.bloqueadoAte,
            tentativasFalhas = this.tentativasFalhas,
            metadata = this.metadata.toMap(),
            roles =
                this.usuarioRoles
                    .map { ur ->
                        UsuarioRole(
                            role =
                                Role(
                                    id = ur.role.id,
                                    code = ur.role.code,
                                    descricao = ur.role.descricao,
                                    authorities =
                                        ur.role.authorities
                                            .map { a ->
                                                Authority(id = a.id, code = a.code, descricao = a.descricao)
                                            }.toSet(),
                                ),
                            escopo = ur.escopo,
                        )
                    }.toSet(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
        )
}
