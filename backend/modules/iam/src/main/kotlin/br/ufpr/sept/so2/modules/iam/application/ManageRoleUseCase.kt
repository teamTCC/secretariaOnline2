package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.AuthorityResponse
import br.ufpr.sept.so2.modules.iam.api.dto.RoleResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.AuthorityJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val PROTECTED_ROLES = setOf("ALUNO", "ADMIN", "SECRETARIO", "PROFESSOR")

@Service
@Transactional
class ManageRoleUseCase(
    private val roleRepo: RoleJpaRepository,
    private val authorityRepo: AuthorityJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
) {
    fun listRoles(): List<RoleResponse> =
        roleRepo.findAll().map { r ->
            RoleResponse(
                id = r.id,
                code = r.code,
                descricao = r.descricao,
                authorities = r.authorities.map { it.code },
            )
        }

    fun listAuthorities(): List<AuthorityResponse> =
        authorityRepo.findAll().map { a ->
            AuthorityResponse(id = a.id, code = a.code, descricao = a.descricao)
        }

    fun createRole(code: String, descricao: String): RoleResponse {
        val normalized = code.uppercase()
        require(roleRepo.findByCode(normalized).isEmpty) { "Role já existe: $normalized" }
        val saved = roleRepo.save(RoleEntity(code = normalized, descricao = descricao))
        return RoleResponse(id = saved.id, code = saved.code, descricao = saved.descricao)
    }

    fun updateRole(id: UUID, descricao: String?): RoleResponse {
        val role = roleRepo.findById(id).orElseThrow { NoSuchElementException("Role não encontrada: $id") }
        descricao?.let { role.descricao = it }
        roleRepo.save(role)
        return RoleResponse(id = role.id, code = role.code, descricao = role.descricao)
    }

    fun setAuthorities(roleId: UUID, authorityCodes: List<String>): RoleResponse {
        val role = roleRepo.findById(roleId).orElseThrow { NoSuchElementException("Role não encontrada: $roleId") }
        val found = authorityRepo.findAllByCodeIn(authorityCodes)
        require(found.size == authorityCodes.distinct().size) {
            "Uma ou mais authorities não existem: $authorityCodes"
        }
        role.authorities.clear()
        role.authorities.addAll(found)
        val saved = roleRepo.save(role)
        return RoleResponse(
            id = saved.id,
            code = saved.code,
            descricao = saved.descricao,
            authorities = saved.authorities.map { it.code },
        )
    }

    fun setUserRoles(userId: UUID, roleCodes: List<String>): List<String> {
        val usuario =
            usuarioRepo.findByIdWithRoles(userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $userId") }
        val roles = roleCodes.map { code ->
            roleRepo.findByCode(code.uppercase()).orElseThrow { NoSuchElementException("Role não encontrada: $code") }
        }
        usuario.usuarioRoles.clear()
        roles.forEach { usuario.usuarioRoles.add(UsuarioRoleEntity(usuario = usuario, role = it)) }
        usuarioRepo.save(usuario)
        return usuario.usuarioRoles.map { it.role.code }
    }

    fun deleteRole(roleId: UUID) {
        val role = roleRepo.findById(roleId).orElseThrow { NoSuchElementException("Role não encontrada: $roleId") }
        require(role.code !in PROTECTED_ROLES) { "Não é permitido excluir o perfil ${role.code}." }
        roleRepo.delete(role)
    }
}
