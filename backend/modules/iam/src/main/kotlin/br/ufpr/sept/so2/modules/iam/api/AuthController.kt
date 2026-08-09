package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.FirstAccessRequest
import br.ufpr.sept.so2.modules.iam.api.dto.ForgotPasswordRequest
import br.ufpr.sept.so2.modules.iam.api.dto.LoginRequest
import br.ufpr.sept.so2.modules.iam.api.dto.LoginResponse
import br.ufpr.sept.so2.modules.iam.api.dto.RefreshTokenRequest
import br.ufpr.sept.so2.modules.iam.api.dto.RefreshTokenResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ResetPasswordRequest
import br.ufpr.sept.so2.modules.iam.application.FirstAccessCommand
import br.ufpr.sept.so2.modules.iam.application.FirstAccessUseCase
import br.ufpr.sept.so2.modules.iam.application.ForgotPasswordCommand
import br.ufpr.sept.so2.modules.iam.application.ForgotPasswordUseCase
import br.ufpr.sept.so2.modules.iam.application.LoginCommand
import br.ufpr.sept.so2.modules.iam.application.LoginUseCase
import br.ufpr.sept.so2.modules.iam.application.RefreshTokenCommand
import br.ufpr.sept.so2.modules.iam.application.RefreshTokenUseCase
import br.ufpr.sept.so2.modules.iam.application.ResetPasswordCommand
import br.ufpr.sept.so2.modules.iam.application.ResetPasswordUseCase
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints públicos de autenticação e gerenciamento de senhas")
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val firstAccessUseCase: FirstAccessUseCase,
) {
    @PostMapping("/login")
    @Operation(
        summary = "Autenticar usuário",
        description = "Autentica via email/GRR + senha. Retorna access token (JWT 15min) e refresh token (7 dias).",
    )
    @ApiResponse(responseCode = "200", description = "Login bem-sucedido")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @ApiResponse(responseCode = "429", description = "Muitas tentativas — rate limit atingido")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<LoginResponse> {
        val result =
            loginUseCase.execute(
                LoginCommand(
                    identificador = request.identificador,
                    senha = request.senha,
                    ip = httpRequest.remoteAddr,
                    userAgent = httpRequest.getHeader("User-Agent"),
                ),
            )

        // Set refresh token in httpOnly cookie
        val cookie =
            Cookie("refresh_token", result.refreshToken).apply {
                isHttpOnly = true
                secure = true
                path = "/auth"
                maxAge = 7 * 24 * 60 * 60
                setAttribute("SameSite", "Lax")
            }
        httpResponse.addCookie(cookie)

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = result.accessToken,
                mustChangePassword = result.mustChangePassword,
                mustAcceptLgpd = result.mustAcceptLgpd,
            ),
        )
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token usando refresh token")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<RefreshTokenResponse> {
        val result =
            refreshTokenUseCase.execute(
                RefreshTokenCommand(
                    refreshTokenValue = request.refreshToken,
                    ip = httpRequest.remoteAddr,
                ),
            )

        val cookie =
            Cookie("refresh_token", result.refreshToken).apply {
                isHttpOnly = true
                secure = true
                path = "/auth"
                maxAge = 7 * 24 * 60 * 60
                setAttribute("SameSite", "Lax")
            }
        httpResponse.addCookie(cookie)

        return ResponseEntity.ok(
            RefreshTokenResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
            ),
        )
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Solicitar redefinição de senha",
        description = "Envia link de redefinição por email. Sempre responde 202 (anti-enumeração).",
    )
    @ApiResponse(responseCode = "202", description = "Requisição recebida")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, String>> {
        forgotPasswordUseCase.execute(
            ForgotPasswordCommand(
                email = request.email,
                ip = httpRequest.remoteAddr,
            ),
        )
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(mapOf("mensagem" to "Se este email existir, enviaremos um link válido por 24h."))
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha com token de email")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, String>> {
        resetPasswordUseCase.execute(
            ResetPasswordCommand(
                token = request.token,
                novaSenha = request.novaSenha,
                ip = httpRequest.remoteAddr,
            ),
        )
        return ResponseEntity.ok(mapOf("mensagem" to "Senha redefinida com sucesso. Faça login novamente."))
    }

    @PostMapping("/first-access")
    @Operation(
        summary = "Completar primeiro acesso",
        description = "Define senha permanente e aceita política de privacidade no primeiro login.",
    )
    fun firstAccess(
        @Valid @RequestBody request: FirstAccessRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, String>> {
        firstAccessUseCase.execute(
            FirstAccessCommand(
                usuarioId = currentUserId(),
                novaSenha = request.novaSenha,
                aceiteLgpd = request.aceiteLgpd,
                ip = httpRequest.remoteAddr,
            ),
        )
        return ResponseEntity.ok(mapOf("mensagem" to "Primeiro acesso concluído com sucesso."))
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão")
    fun logout(httpResponse: HttpServletResponse): ResponseEntity<Map<String, String>> {
        val cookie =
            Cookie("refresh_token", "").apply {
                isHttpOnly = true
                secure = true
                path = "/auth"
                maxAge = 0
            }
        httpResponse.addCookie(cookie)
        return ResponseEntity.ok(mapOf("mensagem" to "Sessão encerrada com sucesso."))
    }
}
