package br.ufpr.sept.so2.modules.iam.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "Identificador é obrigatório")
    val identificador: String,
    @field:NotBlank(message = "Senha é obrigatória")
    val senha: String,
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val mustChangePassword: Boolean,
    val mustAcceptLgpd: Boolean,
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token é obrigatório")
    val refreshToken: String,
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
)

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Formato de email inválido")
    val email: String,
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Token é obrigatório")
    val token: String,
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 12, message = "Senha deve ter no mínimo 12 caracteres")
    val novaSenha: String,
)

data class FirstAccessRequest(
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 12, message = "Senha deve ter no mínimo 12 caracteres")
    val novaSenha: String,
    val aceiteLgpd: Boolean,
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Senha atual é obrigatória")
    val senhaAtual: String,
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 12, message = "Senha deve ter no mínimo 12 caracteres")
    val novaSenha: String,
)
