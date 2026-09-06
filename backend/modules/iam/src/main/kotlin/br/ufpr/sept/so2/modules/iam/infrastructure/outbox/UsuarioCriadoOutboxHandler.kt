package br.ufpr.sept.so2.modules.iam.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UsuarioCriadoOutboxHandler(
    private val mailService: MailService,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean = eventType == OutboxEventTypes.USUARIO_CRIADO

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val email =
            payload["email"]?.toString()
                ?: run {
                    log.warn("UsuarioCriadoOutboxHandler: payload.email ausente no outbox {}", aggregateId)
                    return
                }
        val nome = payload["nome"]?.toString().orEmpty()

        mailService.sendWelcomeEmail(to = email, nome = nome)
    }
}
