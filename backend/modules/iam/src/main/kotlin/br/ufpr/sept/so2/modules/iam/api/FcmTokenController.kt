package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.FcmRegisteredResponse
import br.ufpr.sept.so2.modules.iam.api.dto.FcmUnregisteredResponse
import br.ufpr.sept.so2.modules.iam.api.dto.RegisterFcmTokenDto
import br.ufpr.sept.so2.modules.iam.api.dto.UnregisterFcmTokenDto
import br.ufpr.sept.so2.modules.iam.application.FcmTokenUseCase
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/me")
@Tag(name = "Push FCM", description = "Registro e remoção de tokens FCM para notificações push")
class FcmTokenController(
    private val fcmTokenUseCase: FcmTokenUseCase,
) {
    @PostMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar ou atualizar token FCM do dispositivo (multi-device)")
    fun register(
        @Valid @RequestBody dto: RegisterFcmTokenDto,
    ): ResponseEntity<FcmRegisteredResponse> {
        fcmTokenUseCase.register(currentUserId(), dto.fcmToken, dto.plataforma)
        return ResponseEntity.ok(FcmRegisteredResponse(registered = true))
    }

    @DeleteMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Desregistrar token FCM (logout do dispositivo)")
    fun unregister(
        @Valid @RequestBody dto: UnregisterFcmTokenDto,
    ): ResponseEntity<FcmUnregisteredResponse> {
        fcmTokenUseCase.unregister(currentUserId(), dto.fcmToken)
        return ResponseEntity.ok(FcmUnregisteredResponse(unregistered = true))
    }
}
