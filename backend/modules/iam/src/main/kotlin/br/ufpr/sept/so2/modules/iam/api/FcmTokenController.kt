package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FcmTokenEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FcmTokenJpaRepository
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class RegisterFcmTokenDto(
    @field:NotBlank val fcmToken: String,
    val plataforma: String = "ANDROID",
)

data class UnregisterFcmTokenDto(
    @field:NotBlank val fcmToken: String,
)

@RestController
@RequestMapping("/me")
@Tag(name = "Push FCM", description = "Registro e remoção de tokens FCM para notificações push")
class FcmTokenController(
    private val fcmTokenRepo: FcmTokenJpaRepository,
) {
    @PostMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar ou atualizar token FCM do dispositivo (multi-device)")
    @Transactional
    fun register(
        @Valid @RequestBody dto: RegisterFcmTokenDto,
    ): ResponseEntity<Map<String, Any>> {
        val idUsuario = currentUserId()

        val existing = fcmTokenRepo.findByIdUsuarioAndFcmToken(idUsuario, dto.fcmToken)
        if (existing.isPresent) {
            val token = existing.get()
            token.ativo = true
            token.plataforma = dto.plataforma
            fcmTokenRepo.save(token)
        } else {
            fcmTokenRepo.save(
                FcmTokenEntity(
                    idUsuario = idUsuario,
                    fcmToken = dto.fcmToken,
                    plataforma = dto.plataforma,
                    ativo = true,
                ),
            )
        }

        return ResponseEntity.ok(mapOf("registered" to true))
    }

    @DeleteMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Desregistrar token FCM (logout do dispositivo)")
    @Transactional
    fun unregister(
        @Valid @RequestBody dto: UnregisterFcmTokenDto,
    ): ResponseEntity<Map<String, Any>> {
        val idUsuario = currentUserId()
        fcmTokenRepo.findByIdUsuarioAndFcmToken(idUsuario, dto.fcmToken).ifPresent { token ->
            token.ativo = false
            fcmTokenRepo.save(token)
        }
        return ResponseEntity.ok(mapOf("unregistered" to true))
    }
}
