package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenServicePort
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
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
    private val tokenService: TokenServicePort,
    private val outboxPublisher: OutboxEventPublisher,
    private val auditPublisher: AuditPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: ForgotPasswordCommand) {
        // Always respond the same way — no enumeration of existing accounts
        val usuario = usuarioRepository.findByEmail(command.email.trim().lowercase())

        if (usuario != null && usuario.ativo) {
            val token =
                tokenService.issueOneTimeToken(
                    subject = usuario.id,
                    audience = "password-reset",
                    ttl = Duration.ofHours(24),
                )

            // Same TX as the audit trail: if COMMIT fails, no e-mail is ever sent.
            outboxPublisher.enqueue(
                eventType = OutboxEventTypes.PASSWORD_RESET_REQUESTED,
                aggregateType = "Usuario",
                aggregateId = usuario.id,
                payload =
                    mapOf(
                        "email" to usuario.email.value,
                        "nome" to usuario.nome,
                        "token" to token,
                    ),
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

            log.info("Recuperação de senha enfileirada no outbox para usuario={}", usuario.id)
        } else {
            log.debug("Tentativa de recuperação de senha para email não cadastrado: {}", command.email)
        }
    }
}
