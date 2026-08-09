package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

data class ForgotPasswordCommand(
    val email: String,
    val ip: String?,
)

@Service
class ForgotPasswordUseCase(
    private val usuarioRepository: UsuarioRepository,
    private val jwtTokenService: JwtTokenService,
    private val mailService: MailService,
    private val auditPublisher: AuditPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: ForgotPasswordCommand) {
        // Always respond the same way — no enumeration of existing accounts
        val usuario = usuarioRepository.findByEmail(command.email.trim().lowercase())

        if (usuario != null && usuario.ativo) {
            val token =
                jwtTokenService.issueOneTimeToken(
                    subject = usuario.id,
                    audience = "password-reset",
                    ttl = Duration.ofHours(24),
                )

            mailService.sendPasswordResetEmail(
                to = usuario.email.value,
                nome = usuario.nome,
                token = token,
            )

            auditPublisher.publish(
                AuditPayload(
                    acao = "PASSWORD_RESET_REQUESTED",
                    idAtor = usuario.id,
                    alvoTipo = "usuario",
                    alvoId = usuario.id,
                    ip = command.ip,
                    userAgent = null,
                    resultado = "OK",
                ),
            )

            log.info("Email de recuperação de senha enviado para usuario={}", usuario.id)
        } else {
            // Log for security monitoring, but don't reveal to caller
            log.debug("Tentativa de recuperação de senha para email não cadastrado: {}", command.email)
        }
        // Always returns 202 Accepted — no information leak
    }
}
