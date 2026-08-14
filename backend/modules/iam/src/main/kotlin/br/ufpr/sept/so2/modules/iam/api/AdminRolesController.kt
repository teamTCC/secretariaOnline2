package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.AuthorityJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateRoleDto(
    @field:NotBlank val code: String,
    @field:NotBlank val descricao: String,
)

data class UpdateRoleDto(
    val descricao: String?,
)

data class RoleAuthoritiesDto(
    @field:NotEmpty val authorityCodes: List<String>,
)

data class UserRolesDto(
    @field:NotEmpty val roleCodes: List<String>,
)

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin — IAM", description = "CRUD de perfis, autoridades e atribuição de roles")
@PreAuthorize("hasAuthority('iam.manage_roles') or hasAuthority('system.admin')")
class AdminRolesController(
    private val roleRepo: RoleJpaRepository,
    private val authorityRepo: AuthorityJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
) {
    @GetMapping("/roles", "/perfis")
    @Operation(summary = "Listar perfis (roles) com authorities")
    fun listRoles(): List<Map<String, Any?>> =
        roleRepo.findAll().map { r ->
            mapOf(
                "id" to r.id,
                "code" to r.code,
                "descricao" to r.descricao,
                "authorities" to r.authorities.map { it.code },
            )
        }

    @PostMapping("/roles", "/perfis")
    @Operation(summary = "Criar perfil")
    fun createRole(
        @Valid @RequestBody dto: CreateRoleDto,
    ): ResponseEntity<Map<String, Any?>> {
        val code = dto.code.uppercase()
        require(roleRepo.findByCode(code).isEmpty) { "Role já existe: $code" }
        val saved = roleRepo.save(RoleEntity(code = code, descricao = dto.descricao))
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "code" to saved.code, "descricao" to saved.descricao),
        )
    }

    @PatchMapping("/roles/{id}", "/perfis/{id}")
    @Operation(summary = "Atualizar descrição do perfil")
    fun updateRole(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateRoleDto,
    ): ResponseEntity<Map<String, Any?>> {
        val role = roleRepo.findById(id).orElseThrow { NoSuchElementException("Role não encontrada: $id") }
        dto.descricao?.let { role.descricao = it }
        roleRepo.save(role)
        return ResponseEntity.ok(mapOf("id" to role.id, "code" to role.code, "descricao" to role.descricao))
    }

    @DeleteMapping("/roles/{id}", "/perfis/{id}")
    @Operation(summary = "Excluir perfil sem usuários vinculados")
    fun deleteRole(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val role = roleRepo.findById(id).orElseThrow { NoSuchElementException("Role não encontrada: $id") }
        require(role.code !in PROTECTED_ROLES) { "Não é permitido excluir o perfil ${role.code}." }
        roleRepo.delete(role)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/autoridades")
    @Operation(summary = "Catálogo de authorities FGAC")
    fun listAuthorities(): List<Map<String, Any?>> =
        authorityRepo.findAll().map { a ->
            mapOf("id" to a.id, "code" to a.code, "descricao" to a.descricao)
        }

    @PatchMapping("/roles/{id}/authorities", "/perfis/{id}/authorities")
    @Operation(summary = "Substituir matriz de authorities do perfil")
    @Transactional
    fun setAuthorities(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RoleAuthoritiesDto,
    ): ResponseEntity<Map<String, Any?>> {
        val role = roleRepo.findById(id).orElseThrow { NoSuchElementException("Role não encontrada: $id") }
        val found = authorityRepo.findAllByCodeIn(dto.authorityCodes)
        require(found.size == dto.authorityCodes.distinct().size) {
            "Uma ou mais authorities não existem: ${dto.authorityCodes}"
        }
        role.authorities.clear()
        role.authorities.addAll(found)
        roleRepo.save(role)
        return ResponseEntity.ok(
            mapOf("id" to role.id, "code" to role.code, "authorities" to role.authorities.map { it.code }),
        )
    }

    @PutMapping("/usuarios/{id}/roles")
    @Operation(summary = "Substituir papéis de um usuário")
    @Transactional
    fun setUserRoles(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UserRolesDto,
    ): ResponseEntity<Map<String, Any?>> {
        val usuario =
            usuarioRepo.findByIdWithRoles(id).orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }
        val roles = dto.roleCodes.map { code ->
            roleRepo.findByCode(code.uppercase()).orElseThrow { NoSuchElementException("Role não encontrada: $code") }
        }
        usuario.usuarioRoles.clear()
        roles.forEach { usuario.usuarioRoles.add(UsuarioRoleEntity(usuario = usuario, role = it)) }
        usuarioRepo.save(usuario)
        return ResponseEntity.ok(
            mapOf("id" to usuario.id, "roles" to usuario.usuarioRoles.map { it.role.code }),
        )
    }

    companion object {
        private val PROTECTED_ROLES = setOf("ALUNO", "ADMIN", "SECRETARIO", "PROFESSOR")
    }
}
