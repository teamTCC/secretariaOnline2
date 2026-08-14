package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
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
import java.time.Duration
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
    private val usuarioRepo: UsuarioJpaRepository,
    private val roleRepo: RoleJpaRepository,
    private val argon2PasswordService: Argon2PasswordService,
    private val jwtTokenService: JwtTokenService,
    private val outboxRepo: OutboxEventJpaRepository,
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
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(usuarioRepo.searchUsuarios(nome, email, ativo, pageable)) { u ->
            mapOf(
                "id" to u.id,
                "nome" to u.nome,
                "email" to u.email,
                "grr" to u.grr,
                "ativo" to u.ativo,
                "roles" to u.usuarioRoles.map { it.role.code },
            )
        }

    @PostMapping
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('user.manage_all')")
    @Operation(summary = "Criar novo usuário (secretaria cria alunos/professores)")
    fun create(
        @Valid @RequestBody dto: CreateUsuarioDto,
    ): ResponseEntity<Map<String, Any>> {
        require(!usuarioRepo.existsByEmail(dto.email)) { "Email já cadastrado: ${dto.email}" }
        dto.grr?.let { require(!usuarioRepo.existsByGrr(it)) { "GRR já cadastrado: $it" } }

        val role =
            roleRepo
                .findByCode(dto.roleCode)
                .orElseThrow { NoSuchElementException("Role não encontrada: ${dto.roleCode}") }

        val senhaTemporaria = UUID.randomUUID().toString().take(12)
        val senhaHash = argon2PasswordService.hash(senhaTemporaria)

        val usuario =
            UsuarioEntity(
                nome = dto.nome,
                email = dto.email,
                grr = dto.grr,
                senhaHash = senhaHash,
                senhaAlterada = false,
            )
        val saved = usuarioRepo.save(usuario)

        val usuarioRole = UsuarioRoleEntity(usuario = saved, role = role)
        saved.usuarioRoles.add(usuarioRole)
        usuarioRepo.save(saved)

        outboxRepo.save(
            OutboxEventEntity(
                eventType = "iam.usuario_criado",
                aggregateType = "Usuario",
                aggregateId = saved.id,
                payload =
                    mapOf(
                        "email" to dto.email,
                        "nome" to dto.nome,
                        "senhaTemporaria" to senhaTemporaria,
                    ),
            ),
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "email" to saved.email),
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('user.manage_all')")
    @Operation(summary = "Detalhe de um usuário")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val usuario =
            usuarioRepo
                .findByIdWithRoles(id)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }

        return ResponseEntity.ok(
            mapOf(
                "id" to usuario.id,
                "nome" to usuario.nome,
                "email" to usuario.email,
                "grr" to usuario.grr,
                "ativo" to usuario.ativo,
                "metadata" to usuario.metadata,
                "roles" to usuario.usuarioRoles.map { it.role.code },
                "senhaAlterada" to usuario.senhaAlterada,
            ),
        )
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('user.manage_all')")
    @Operation(summary = "Ativar ou desativar usuário")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody dto: UpdateStatusDto,
    ): ResponseEntity<Map<String, Any?>> {
        val usuario =
            usuarioRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }

        usuario.ativo = dto.ativo
        usuarioRepo.save(usuario)

        return ResponseEntity.ok(mapOf("id" to usuario.id, "ativo" to usuario.ativo))
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user.reset_password')")
    @Operation(summary = "Resetar senha do usuário (envia link por e-mail)")
    fun resetPassword(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, String>> {
        val usuario =
            usuarioRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }

        require(usuario.ativo) { "Não é possível resetar senha de usuário inativo." }

        val token =
            jwtTokenService.issueOneTimeToken(
                subject = usuario.id,
                audience = "password-reset",
                ttl = Duration.ofHours(24),
            )

        outboxRepo.save(
            OutboxEventEntity(
                eventType = OutboxEventTypes.PASSWORD_RESET_REQUESTED,
                aggregateType = "Usuario",
                aggregateId = usuario.id,
                payload =
                    mapOf(
                        "email" to usuario.email,
                        "nome" to usuario.nome,
                        "token" to token,
                    ),
            ),
        )

        return ResponseEntity.ok(mapOf("mensagem" to "Link de redefinição de senha enviado para ${usuario.email}."))
    }
}
