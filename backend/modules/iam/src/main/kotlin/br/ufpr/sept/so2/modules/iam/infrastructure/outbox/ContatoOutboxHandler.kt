package br.ufpr.sept.so2.modules.iam.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.config.ContatoProperties
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ContatoOutboxHandler(
    private val mailService: MailService,
    private val contatoProperties: ContatoProperties,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean = eventType == OutboxEventTypes.CONTATO_RECEBIDO

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val nome = payload["nome"]?.toString().orEmpty().escapeHtml()
        val email = payload["email"]?.toString().orEmpty().escapeHtml()
        val assunto = payload["assunto"]?.toString().orEmpty().escapeHtml()
        val mensagem = payload["mensagem"]?.toString().orEmpty().escapeHtml()
        val dest = contatoProperties.email
        if (dest.isBlank()) {
            log.warn("app.contato.email vazio — mensagem {} não encaminhada", aggregateId)
            return
        }
        mailService.sendNotificationEmail(
            to = dest,
            subject = "Contato SEPT — $assunto",
            html =
                """
                <html><body>
                <h2>Nova mensagem de contato</h2>
                <p><strong>De:</strong> $nome &lt;$email&gt;</p>
                <p><strong>Assunto:</strong> $assunto</p>
                <p>${mensagem.replace("\n", "<br/>")}</p>
                </body></html>
                """.trimIndent(),
        )
    }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
