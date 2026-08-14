package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.iam.application.DataExportUseCase
import br.ufpr.sept.so2.modules.iam.application.RequestDataExportCommand
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.NotificationPrefEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.NotificationPrefJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class UpdateProfileDto(
    val nome: String?,
    val metadata: Map<String, Any>?,
)

data class ChangePasswordDto(
    @field:NotBlank val senhaAtual: String,
    @field:NotBlank @field:Size(min = 8) val novaSenha: String,
)

data class UpdateNotificationsDto(
    val emailEnabled: Boolean?,
    val pushEnabled: Boolean?,
    val inAppEnabled: Boolean?,
)

@RestController
@RequestMapping("/me")
@Tag(name = "Perfil", description = "Gerenciamento do perfil do usuário autenticado")
class ProfileController(
    private val dataExportUseCase: DataExportUseCase,
    private val usuarioJpaRepository: UsuarioJpaRepository,
    private val argon2PasswordService: Argon2PasswordService,
    private val notificationPrefRepo: NotificationPrefJpaRepository,
    private val minioStorageService: MinioStorageService,
) {

    @GetMapping
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(
        summary = "Perfil do usuário autenticado",
        description = "Retorna dados pessoais, roles e links HATEOAS disponíveis.",
    )
    fun getProfile(): EntityModel<Map<String, Any?>> {
        val userId = currentUserId()
        val usuario =
            usuarioJpaRepository
                .findByIdWithRoles(userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $userId") }

        val roles = usuario.usuarioRoles.map { it.role.code }

        val body: Map<String, Any?> =
            mapOf(
                "id" to usuario.id,
                "nome" to usuario.nome,
                "email" to usuario.email,
                "grr" to usuario.grr,
                "ativo" to usuario.ativo,
                "metadata" to usuario.metadata,
                "roles" to roles,
            )

        val model = EntityModel.of(body)
        model.add(Link.of("/me").withSelfRel())
        model.add(Link.of("/me").withRel("update-profile").withType("PATCH"))
        model.add(Link.of("/me/password").withRel("change-password").withType("POST"))
        model.add(Link.of("/me/notifications").withRel("notifications").withType("PATCH"))
        model.add(Link.of("/me/data-export").withRel("data-export").withType("POST"))

        return model
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(summary = "Atualizar dados pessoais (nome, metadata)")
    fun updateProfile(
        @RequestBody dto: UpdateProfileDto,
    ): ResponseEntity<Map<String, Any?>> {
        val userId = currentUserId()
        val usuario =
            usuarioJpaRepository
                .findById(userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $userId") }

        dto.nome?.let { usuario.nome = it }
        dto.metadata?.let { usuario.metadata = it.toMutableMap() }
        val saved = usuarioJpaRepository.save(usuario)

        return ResponseEntity.ok(mapOf("id" to saved.id, "nome" to saved.nome))
    }

    @PostMapping("/password")
    @PreAuthorize("hasAuthority('user.update_own_password')")
    @Operation(summary = "Alterar senha do usuário autenticado")
    @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso")
    @ApiResponse(responseCode = "400", description = "Senha atual incorreta")
    fun changePassword(
        @Valid @RequestBody dto: ChangePasswordDto,
    ): ResponseEntity<Void> {
        val userId = currentUserId()
        val usuario =
            usuarioJpaRepository
                .findById(userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $userId") }

        require(argon2PasswordService.verify(dto.senhaAtual, usuario.senhaHash)) {
            "Senha atual incorreta."
        }

        val newHash = argon2PasswordService.hash(dto.novaSenha)
        usuarioJpaRepository.updatePassword(userId, newHash)

        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/notifications")
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(summary = "Atualizar preferências de notificação")
    fun updateNotifications(
        @RequestBody dto: UpdateNotificationsDto,
    ): ResponseEntity<Map<String, Any?>> {
        val userId = currentUserId()
        val pref =
            notificationPrefRepo.findByIdUsuario(userId).orElseGet {
                NotificationPrefEntity(idUsuario = userId)
            }

        dto.emailEnabled?.let { pref.emailEnabled = it }
        dto.pushEnabled?.let { pref.pushEnabled = it }
        dto.inAppEnabled?.let { pref.inAppEnabled = it }
        val saved = notificationPrefRepo.save(pref)

        return ResponseEntity.ok(
            mapOf(
                "emailEnabled" to saved.emailEnabled,
                "pushEnabled" to saved.pushEnabled,
                "inAppEnabled" to saved.inAppEnabled,
            ),
        )
    }

    @PostMapping("/avatar")
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(
        summary = "Obter URL pré-assinada para upload de avatar",
        description = "Retorna URL PUT válida por 15 minutos para envio direto ao MinIO.",
    )
    fun requestAvatarUpload(): ResponseEntity<Map<String, String>> {
        val userId = currentUserId()
        val storageKey = "avatars/$userId.jpg"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, "image/jpeg", 15)

        return ResponseEntity.ok(
            mapOf(
                "uploadUrl" to uploadUrl,
                "storageKey" to storageKey,
            ),
        )
    }

    // RF-F1-003-d — Solicitar exportação de dados pessoais (LGPD Art. 18, III)
    @PostMapping("/data-export")
    @PreAuthorize("hasAuthority('user.export_own_data')")
    @Operation(
        summary = "Solicitar exportação de dados pessoais",
        description = """
            Gera arquivo JSON com todos os dados pessoais do usuário (LGPD Art. 18, III).
            Retorna 202 com jobId e downloadUrl pré-assinada válida por 24 horas.
        """,
    )
    @ApiResponse(responseCode = "202", description = "Exportação gerada — use downloadUrl para baixar o arquivo")
    fun requestDataExport(httpRequest: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        val result =
            dataExportUseCase.requestExport(
                RequestDataExportCommand(
                    usuarioId = currentUserId(),
                    ip = httpRequest.remoteAddr,
                    userAgent = httpRequest.getHeader("User-Agent"),
                ),
            )
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(
                mapOf(
                    "jobId" to result.jobId.toString(),
                    "downloadUrl" to result.downloadUrl,
                ),
            )
    }

    // RF-F1-003-d — Verificar status do job de exportação
    @GetMapping("/data-export/{jobId}")
    @PreAuthorize("hasAuthority('user.export_own_data')")
    @Operation(
        summary = "Verificar status da exportação de dados",
        description = "Retorna PENDING, READY (com URL pré-assinada por 24h) ou EXPIRED.",
    )
    @ApiResponse(responseCode = "200", description = "Status do job de exportação")
    @ApiResponse(responseCode = "404", description = "Job não encontrado ou pertence a outro usuário")
    fun getDataExportStatus(
        @PathVariable jobId: String,
    ): ResponseEntity<Map<String, Any?>> {
        val result =
            dataExportUseCase.getExportStatus(
                usuarioId = currentUserId(),
                jobId = jobId,
            )
        return ResponseEntity.ok(
            mapOf(
                "jobId" to result.jobId.toString(),
                "status" to result.status.name,
                "downloadUrl" to result.downloadUrl,
                "expiresAt" to result.expiresAt?.toString(),
            ),
        )
    }
}
