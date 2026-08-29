package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.CsrfResponse
import br.ufpr.sept.so2.modules.iam.api.dto.FirstAccessRequest
import br.ufpr.sept.so2.modules.iam.api.dto.ForgotPasswordRequest
import br.ufpr.sept.so2.modules.iam.api.dto.LoginRequest
import br.ufpr.sept.so2.modules.iam.api.dto.LoginResponse
import br.ufpr.sept.so2.modules.iam.api.dto.MessageResponse
import br.ufpr.sept.so2.modules.iam.api.dto.OttExchangeRequest
import br.ufpr.sept.so2.modules.iam.api.dto.RefreshResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ResetPasswordRequest
import br.ufpr.sept.so2.modules.iam.application.ExchangeOttCommand
import br.ufpr.sept.so2.modules.iam.application.ExchangeOttUseCase
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
import br.ufpr.sept.so2.modules.iam.application.ports.out.RefreshTokenRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenRevocationPort
import br.ufpr.sept.so2.modules.iam.domain.exceptions.InvalidTokenException
import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(
    name = "Autenticação",
    description = "Login/refresh/logout via HttpOnly cookies. Todos os tokens trafegam apenas em cookies — nunca no corpo JSON.",
)
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val exchangeOttUseCase: ExchangeOttUseCase,
    private val firstAccessUseCase: FirstAccessUseCase,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenService: JwtTokenService,
    private val tokenRevocationPort: TokenRevocationPort,
    @Value("\${app.security.cookie.secure:true}") private val cookieSecure: Boolean,
    @Value("\${app.security.cookie.same-site:Lax}") private val cookieSameSite: String,
) {
    @GetMapping("/csrf")
    @SecurityRequirements
    @Operation(summary = "Emitir cookie XSRF-TOKEN (Double Submit) e devolver o valor do token")
    fun csrf(csrfToken: CsrfToken): CsrfResponse =
        CsrfResponse(
            token = csrfToken.token,
            headerName = csrfToken.headerName,
            parameterName = csrfToken.parameterName,
        )

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
        summary = "Autenticar usuário",
        description = """
            Autentica via email/GRR + senha.
            Tokens são entregues em cookies HttpOnly — NÃO aparecem no corpo JSON.
            access_token: Path=/, Max-Age=TTL do JWT.
            refresh_token: Path=/auth, Max-Age=7 dias.
        """,
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

        httpResponse.addCookie(accessTokenCookie(result.accessToken))
        httpResponse.addCookie(refreshTokenCookie(result.refreshToken))

        return ResponseEntity.ok(
            LoginResponse(
                mustChangePassword = result.mustChangePassword,
                mustAcceptLgpd = result.mustAcceptLgpd,
            ),
        )
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(
        summary = "Renovar access token",
        description = """
            Lê o refresh_token do cookie HttpOnly e emite um novo par de tokens (rotação).
            Não aceita nem devolve tokens no corpo — ambos trafegam via cookies.
            Reuso do mesmo refresh token dispara revogação de todas as sessões (detecção de roubo).
        """,
    )
    fun refresh(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<RefreshResponse> {
        val refreshTokenValue =
            httpRequest.cookies
                ?.firstOrNull { it.name == "refresh_token" }
                ?.value
                ?.takeIf { it.isNotBlank() }
                ?: throw InvalidTokenException("Refresh token não encontrado. Faça login novamente.")

        val result =
            refreshTokenUseCase.execute(
                RefreshTokenCommand(
                    refreshTokenValue = refreshTokenValue,
                    ip = httpRequest.remoteAddr,
                ),
            )

        httpResponse.addCookie(accessTokenCookie(result.accessToken))
        httpResponse.addCookie(refreshTokenCookie(result.refreshToken))

        return ResponseEntity.ok(RefreshResponse())
    }

    @PostMapping("/ott")
    @SecurityRequirements
    @Operation(
        summary = "Trocar token one-time (deep-link ?ott=) por sessão",
        description = "Consome o JWT de uso único emitido no e-mail da solicitação e define cookies de sessão.",
    )
    @ApiResponse(responseCode = "200", description = "Sessão criada")
    @ApiResponse(responseCode = "401", description = "Token inválido, expirado ou já utilizado")
    fun exchangeOtt(
        @Valid @RequestBody request: OttExchangeRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<LoginResponse> {
        val result =
            exchangeOttUseCase.execute(
                ExchangeOttCommand(
                    token = request.token,
                    ip = httpRequest.remoteAddr,
                    userAgent = httpRequest.getHeader("User-Agent"),
                ),
            )
        httpResponse.addCookie(accessTokenCookie(result.accessToken))
        httpResponse.addCookie(refreshTokenCookie(result.refreshToken))
        return ResponseEntity.ok(
            LoginResponse(
                mustChangePassword = result.mustChangePassword,
                mustAcceptLgpd = result.mustAcceptLgpd,
            ),
        )
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(
        summary = "Solicitar redefinição de senha",
        description = "Envia link de redefinição por email. Sempre responde 202 (anti-enumeração).",
    )
    @ApiResponse(responseCode = "202", description = "Requisição recebida")
    @ApiResponse(responseCode = "429", description = "Muitas tentativas — rate limit atingido")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<MessageResponse> {
        forgotPasswordUseCase.execute(
            ForgotPasswordCommand(
                email = request.email,
                ip = httpRequest.remoteAddr,
            ),
        )
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(MessageResponse("Se este email existir, enviaremos um link válido por 24h."))
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Redefinir senha com token de email")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<MessageResponse> {
        resetPasswordUseCase.execute(
            ResetPasswordCommand(
                token = request.token,
                novaSenha = request.novaSenha,
                ip = httpRequest.remoteAddr,
            ),
        )
        return ResponseEntity.ok(MessageResponse("Senha redefinida com sucesso. Faça login novamente."))
    }

    @PostMapping("/first-access")
    @Operation(
        summary = "Completar primeiro acesso",
        description = "Define senha permanente e aceita política de privacidade no primeiro login.",
    )
    fun firstAccess(
        @Valid @RequestBody request: FirstAccessRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<MessageResponse> {
        firstAccessUseCase.execute(
            FirstAccessCommand(
                usuarioId = currentUserId(),
                novaSenha = request.novaSenha,
                aceiteLgpd = request.aceiteLgpd,
                ip = httpRequest.remoteAddr,
            ),
        )
        return ResponseEntity.ok(MessageResponse("Primeiro acesso concluído com sucesso."))
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Encerrar sessão",
        description = """
            Deleta a sessão Redis (auth:session:<sid>) — invalida o access token imediatamente.
            Revoga todos os refresh tokens do usuário no banco.
            Apaga os cookies access_token e refresh_token.
        """,
    )
    fun logout(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<MessageResponse> {
        val tokenValue =
            httpRequest.cookies?.firstOrNull { it.name == "access_token" }?.value?.takeIf { it.isNotBlank() }
                ?: httpRequest.getHeader("Authorization")
                    ?.let { if (it.startsWith("Bearer ")) it.removePrefix("Bearer ").trim() else null }

        if (tokenValue != null) {
            runCatching { jwtTokenService.verify(tokenValue) }
                .getOrNull()
                ?.payload
                ?.let { claims ->
                    val sid = claims["sid"] as? String
                    if (sid != null) {
                        // Session-based logout: delete Redis key → instant revocation
                        tokenRevocationPort.deleteSession(sid)
                    } else {
                        // Legacy fallback for tokens without sid
                        val jti = claims.id
                        val exp = claims.expiration
                        if (jti != null && exp != null) {
                            tokenRevocationPort.revokeAccessToken(jti, exp)
                        }
                    }
                }
        }

        refreshTokenRepository.revokeAllForUser(currentUserId())

        httpResponse.addCookie(clearCookie("access_token", "/"))
        httpResponse.addCookie(clearCookie("refresh_token", "/auth"))

        return ResponseEntity.ok(MessageResponse("Sessão encerrada com sucesso."))
    }

    // ── Cookie helpers ────────────────────────────────────────────────────────

    private fun accessTokenCookie(value: String): Cookie =
        buildCookie(
            name = "access_token",
            value = value,
            path = "/",
            maxAge = jwtTokenService.accessTtlSeconds.toInt(),
        )

    private fun refreshTokenCookie(value: String): Cookie =
        buildCookie(
            name = "refresh_token",
            value = value,
            path = "/auth",
            maxAge = 7 * 24 * 60 * 60,
        )

    private fun clearCookie(
        name: String,
        path: String,
    ): Cookie = buildCookie(name = name, value = "", path = path, maxAge = 0)

    private fun buildCookie(
        name: String,
        value: String,
        path: String,
        maxAge: Int,
    ): Cookie =
        Cookie(name, value).apply {
            isHttpOnly = true
            // SameSite=None requires Secure; enforce that regardless of the property
            secure = cookieSecure || cookieSameSite.equals("None", ignoreCase = true)
            this.path = path
            this.maxAge = maxAge
            setAttribute("SameSite", cookieSameSite)
        }
}
