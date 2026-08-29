package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.PasswordResetEnqueuedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioCreatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioDetailResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioStatusResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioSummaryResponse
import br.ufpr.sept.so2.modules.iam.application.CreateUsuarioCommand
import br.ufpr.sept.so2.modules.iam.application.ManageUsuarioUseCase
import br.ufpr.sept.so2.modules.iam.application.UsuarioQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateUsuarioDto(
    @field:NotBlank val nome: String,
    @field:NotBlank @field:Email val email: String,
    val grr: String?,
    @field:NotBlank val roleCode: String,
)

data class UpdateStatusDto(
    val ativo: Boolean,
)

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários", description = "Administração de usuários (secretaria/admin)")
class UsuariosController(
    private val usuarioQuery: UsuarioQuery,
    private val manageUsuarioUseCase: ManageUsuarioUseCase,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('user.manage_all')")
    @Operation(summary = "Listar usuários com filtros (nome, email, ativo)")
    fun list(
        @RequestParam(required = false) nome: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) ativo: Boolean?,
        @RequestParam(required = false) role: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<UsuarioSummaryResponse> = usuarioQuery.list(nome, email, ativo, pageable)

    @PostMapping
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('user.manage_all')")
    @Operation(summary = "Criar novo usuário (secretaria cria alunos/professores)")
    fun create(
        @Valid @RequestBody dto: CreateUsuarioDto,
    ): ResponseEntity<UsuarioCreatedResponse> {
        val created =
            manageUsuarioUseCase.create(
                CreateUsuarioCommand(
                    nome = dto.nome,
                    email = dto.email,
                    grr = dto.grr,
                    roleCode = dto.roleCode,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('user.manage_all')")
    @Operation(summary = "Detalhe de um usuário")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<UsuarioDetailResponse> = ResponseEntity.ok(usuarioQuery.getById(id))

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('user.manage_all')")
    @Operation(summary = "Ativar ou desativar usuário")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody dto: UpdateStatusDto,
    ): ResponseEntity<UsuarioStatusResponse> =
        ResponseEntity.ok(manageUsuarioUseCase.updateStatus(id, dto.ativo))

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user.reset_password')")
    @Operation(summary = "Resetar senha do usuário (envia link por e-mail)")
    fun resetPassword(
        @PathVariable id: UUID,
    ): ResponseEntity<PasswordResetEnqueuedResponse> =
        ResponseEntity.ok(manageUsuarioUseCase.enqueuePasswordReset(id))
}
