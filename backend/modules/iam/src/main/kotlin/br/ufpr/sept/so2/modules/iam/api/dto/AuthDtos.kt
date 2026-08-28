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

/**
 * Tokens (access + refresh) are delivered via HttpOnly cookies — NOT in this response body.
 * The body only carries flags needed for the SPA to decide the post-login redirect.
 */
data class LoginResponse(
    val mustChangePassword: Boolean,
    val mustAcceptLgpd: Boolean,
)

/**
 * Refresh endpoint reads the refresh token from cookie and sets new cookies.
 * Body carries only the status message.
 */
data class RefreshResponse(
    val mensagem: String = "Token renovado com sucesso.",
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
