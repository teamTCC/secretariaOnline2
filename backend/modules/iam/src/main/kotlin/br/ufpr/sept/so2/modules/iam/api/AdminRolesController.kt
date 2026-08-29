package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.AuthorityResponse
import br.ufpr.sept.so2.modules.iam.api.dto.RoleResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UserRolesResponse
import br.ufpr.sept.so2.modules.iam.application.ManageRoleUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    private val manageRoleUseCase: ManageRoleUseCase,
) {
    @GetMapping("/roles", "/perfis")
    @Operation(summary = "Listar perfis (roles) com authorities")
    fun listRoles(): List<RoleResponse> = manageRoleUseCase.listRoles()

    @PostMapping("/roles", "/perfis")
    @Operation(summary = "Criar perfil")
    fun createRole(
        @Valid @RequestBody dto: CreateRoleDto,
    ): ResponseEntity<RoleResponse> {
        val saved = manageRoleUseCase.createRole(dto.code, dto.descricao)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PatchMapping("/roles/{id}", "/perfis/{id}")
    @Operation(summary = "Atualizar descrição do perfil")
    fun updateRole(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateRoleDto,
    ): ResponseEntity<RoleResponse> = ResponseEntity.ok(manageRoleUseCase.updateRole(id, dto.descricao))

    @DeleteMapping("/roles/{id}", "/perfis/{id}")
    @Operation(summary = "Excluir perfil sem usuários vinculados")
    fun deleteRole(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        manageRoleUseCase.deleteRole(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/autoridades")
    @Operation(summary = "Catálogo de authorities FGAC")
    fun listAuthorities(): List<AuthorityResponse> = manageRoleUseCase.listAuthorities()

    @PatchMapping("/roles/{id}/authorities", "/perfis/{id}/authorities")
    @Operation(summary = "Substituir matriz de authorities do perfil")
    fun setAuthorities(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RoleAuthoritiesDto,
    ): ResponseEntity<RoleResponse> = ResponseEntity.ok(manageRoleUseCase.setAuthorities(id, dto.authorityCodes))

    @PutMapping("/usuarios/{id}/roles")
    @Operation(summary = "Substituir papéis de um usuário")
    fun setUserRoles(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UserRolesDto,
    ): ResponseEntity<UserRolesResponse> {
        val roleCodes = manageRoleUseCase.setUserRoles(id, dto.roleCodes)
        return ResponseEntity.ok(UserRolesResponse(id = id, roles = roleCodes))
    }
}
