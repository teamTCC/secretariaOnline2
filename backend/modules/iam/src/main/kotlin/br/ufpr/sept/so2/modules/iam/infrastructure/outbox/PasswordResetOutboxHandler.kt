package br.ufpr.sept.so2.modules.iam.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PasswordResetOutboxHandler(
    private val mailService: MailService,
) : OutboxEventHandler {
    override fun supports(eventType: String): Boolean = eventType == OutboxEventTypes.PASSWORD_RESET_REQUESTED

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val email = payload["email"]?.toString() ?: error("payload.email ausente no outbox $aggregateId")
        val nome = payload["nome"]?.toString().orEmpty()
        val token = payload["token"]?.toString() ?: error("payload.token ausente no outbox $aggregateId")
        mailService.sendPasswordResetEmail(to = email, nome = nome, token = token)
    }
}
