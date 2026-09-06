package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.AvatarUploadResponse
import br.ufpr.sept.so2.modules.iam.api.dto.DataExportStartedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.DataExportStatusResponse
import br.ufpr.sept.so2.modules.iam.api.dto.NotificationPrefResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ProfileResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ProfileUpdatedResponse
import br.ufpr.sept.so2.modules.iam.application.ChangePasswordCommand
import br.ufpr.sept.so2.modules.iam.application.DataExportUseCase
import br.ufpr.sept.so2.modules.iam.application.ProfileQuery
import br.ufpr.sept.so2.modules.iam.application.RequestDataExportCommand
import br.ufpr.sept.so2.modules.iam.application.UpdateNotificationsCommand
import br.ufpr.sept.so2.modules.iam.application.UpdateProfileCommand
import br.ufpr.sept.so2.modules.iam.application.UpdateProfileUseCase
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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
    private val profileQuery: ProfileQuery,
    private val updateProfileUseCase: UpdateProfileUseCase,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(
        summary = "Perfil do usuário autenticado",
        description = "Retorna dados pessoais, roles e links HATEOAS disponíveis.",
    )
    fun getProfile(): ProfileResponse = profileQuery.getProfile(currentUserId())

    @PatchMapping
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(summary = "Atualizar dados pessoais (nome, metadata)")
    fun updateProfile(
        @RequestBody dto: UpdateProfileDto,
    ): ResponseEntity<ProfileUpdatedResponse> {
        val saved =
            updateProfileUseCase.updateProfile(
                UpdateProfileCommand(
                    userId = currentUserId(),
                    nome = dto.nome,
                    metadata = dto.metadata,
                ),
            )
        return ResponseEntity.ok(saved)
    }

    @PostMapping("/password")
    @PreAuthorize("hasAuthority('user.update_own_password')")
    @Operation(summary = "Alterar senha do usuário autenticado")
    @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso")
    @ApiResponse(responseCode = "400", description = "Senha atual incorreta")
    fun changePassword(
        @Valid @RequestBody dto: ChangePasswordDto,
    ): ResponseEntity<Void> {
        updateProfileUseCase.changePassword(
            ChangePasswordCommand(
                userId = currentUserId(),
                senhaAtual = dto.senhaAtual,
                novaSenha = dto.novaSenha,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/notifications")
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(summary = "Atualizar preferências de notificação")
    fun updateNotifications(
        @RequestBody dto: UpdateNotificationsDto,
    ): ResponseEntity<NotificationPrefResponse> {
        val saved =
            updateProfileUseCase.updateNotifications(
                UpdateNotificationsCommand(
                    userId = currentUserId(),
                    emailEnabled = dto.emailEnabled,
                    pushEnabled = dto.pushEnabled,
                    inAppEnabled = dto.inAppEnabled,
                ),
            )
        return ResponseEntity.ok(saved)
    }

    @PostMapping("/avatar")
    @PreAuthorize("hasAuthority('user.update_own_profile')")
    @Operation(
        summary = "Obter URL pré-assinada para upload de avatar",
        description = "Retorna URL PUT válida por 15 minutos para envio direto ao MinIO.",
    )
    fun requestAvatarUpload(): ResponseEntity<AvatarUploadResponse> =
        ResponseEntity.ok(updateProfileUseCase.requestAvatarUpload(currentUserId()))

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
    fun requestDataExport(httpRequest: HttpServletRequest): ResponseEntity<DataExportStartedResponse> {
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
            .body(DataExportStartedResponse(jobId = result.jobId.toString(), downloadUrl = result.downloadUrl))
    }

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
    ): ResponseEntity<DataExportStatusResponse> {
        val result =
            dataExportUseCase.getExportStatus(
                usuarioId = currentUserId(),
                jobId = jobId,
            )
        return ResponseEntity.ok(
            DataExportStatusResponse(
                jobId = result.jobId.toString(),
                status = result.status.name,
                downloadUrl = result.downloadUrl,
                expiresAt = result.expiresAt?.toString(),
            ),
        )
    }
}
