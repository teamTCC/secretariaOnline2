package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.RefreshTokenRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.domain.RefreshToken
import br.ufpr.sept.so2.modules.iam.domain.Usuario
import br.ufpr.sept.so2.modules.iam.domain.exceptions.AccountBlockedException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.InvalidCredentialsException
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

data class LoginCommand(
    val identificador: String,
    val senha: String,
    val ip: String?,
    val userAgent: String?,
)

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val mustChangePassword: Boolean,
    val mustAcceptLgpd: Boolean,
)

@Service
class LoginUseCase(
    private val usuarioRepository: UsuarioRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenService: JwtTokenService,
    private val passwordService: Argon2PasswordService,
    private val auditPublisher: AuditPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: LoginCommand): LoginResult {
        val identificador = command.identificador.trim().lowercase()

        val usuario = usuarioRepository.findByIdentificador(identificador)

        // Generic error — no enumeration of existing accounts
        if (usuario == null || !usuario.ativo) {
            auditPublisher.publish(
                AuditPayload(
                    acao = "LOGIN_FAILED",
                    idAtor = null,
                    alvoTipo = "usuario",
                    alvoId = null,
                    ip = command.ip,
                    userAgent = command.userAgent,
                    resultado = "FAILURE",
                    detalhes = mapOf("razao" to "USUARIO_NAO_ENCONTRADO_OU_INATIVO"),
                ),
            )
            throw InvalidCredentialsException()
        }

        if (usuario.estaBloqueado()) {
            val minutesRemaining =
                java.time.temporal.ChronoUnit.MINUTES.between(
                    OffsetDateTime.now(),
                    usuario.bloqueadoAte,
                )
            throw AccountBlockedException(minutesRemaining.coerceAtLeast(1))
        }

        if (!passwordService.verify(command.senha, usuario.senhaHash)) {
            handleFailedAttempt(usuario, command)
            throw InvalidCredentialsException()
        }

        // Success: reset failed attempts counter
        if (usuario.tentativasFalhas > 0) {
            usuarioRepository.updateFailedAttempts(usuario.id, 0, null)
        }

        val accessToken = jwtTokenService.issueAccessToken(usuario)
        val refreshToken = RefreshToken.issue(usuario.id)
        refreshTokenRepository.save(refreshToken)

        auditPublisher.publish(
            AuditPayload(
                acao = "LOGIN_SUCCESS",
                idAtor = usuario.id,
                alvoTipo = "usuario",
                alvoId = usuario.id,
                ip = command.ip,
                userAgent = command.userAgent,
                resultado = "OK",
            ),
        )

        log.info("Login bem-sucedido para usuario={}", usuario.id)

        return LoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken.value,
            mustChangePassword = usuario.mustChangePassword(),
            mustAcceptLgpd = !usuario.aceitouLgpd(),
        )
    }

    private fun handleFailedAttempt(
        usuario: Usuario,
        command: LoginCommand,
    ) {
        val newAttempts = usuario.tentativasFalhas + 1
        val bloqueadoAte =
            if (newAttempts >= Usuario.MAX_FAILED_ATTEMPTS) {
                OffsetDateTime.now().plusMinutes(Usuario.LOCK_DURATION_MINUTES)
            } else {
                null
            }
        usuarioRepository.updateFailedAttempts(usuario.id, newAttempts, bloqueadoAte)

        auditPublisher.publish(
            AuditPayload(
                acao = if (bloqueadoAte != null) "ACCOUNT_BLOCKED" else "LOGIN_FAILED",
                idAtor = usuario.id,
                alvoTipo = "usuario",
                alvoId = usuario.id,
                ip = command.ip,
                userAgent = command.userAgent,
                resultado = "FAILURE",
                detalhes = mapOf("tentativas" to newAttempts, "bloqueado" to (bloqueadoAte != null)),
            ),
        )
    }
}
