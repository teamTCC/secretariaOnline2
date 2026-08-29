package br.ufpr.sept.so2.modules.solicitacoes.infrastructure.outbox

import br.ufpr.sept.so2.modules.comunicacao.application.InAppNotificationService
import br.ufpr.sept.so2.modules.comunicacao.application.TemplateEngine
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.OneTimeTokenPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RequestTransitionOutboxHandler(
    private val usuarioRepository: UsuarioRepository,
    private val mailService: MailService,
    private val templateEngine: TemplateEngine,
    private val inApp: InAppNotificationService,
    private val oneTimeTokenPort: OneTimeTokenPort,
    @Value("\${app.frontend-url:https://secretariaonline.ufpr.br}") private val frontendUrl: String,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean = eventType.startsWith("solicitacoes.")

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val solicitanteId =
            payload["idSolicitante"]?.toString()?.let(UUID::fromString)
                ?: error("payload.idSolicitante ausente no outbox $aggregateId")

        val usuario = usuarioRepository.findById(solicitanteId)
        if (usuario == null || !usuario.ativo) {
            log.warn("Solicitante {} inativo ou inexistente — evento {} ignorado", solicitanteId, eventType)
            return
        }

        val tipo = payload["tipoCode"]?.toString().orEmpty()
        val estadoNovo = payload["estadoNovo"]?.toString().orEmpty()
        val parecer = payload["parecer"]?.toString().orEmpty()
        val parecerHtml =
            if (parecer.isNotBlank()) {
                "<p><strong>Parecer:</strong> ${parecer.escapeHtml()}</p>"
            } else {
                ""
            }

        // When the transition requires a one-time action from the student (e.g., RESUBMIT after
        // REQUEST_ADJUSTMENT), generate a deep-link token so they can jump directly to the request
        // without logging in first.
        val deepLinkHtml = buildDeepLinkHtml(
            generateOneTimeToken = payload["generateOneTimeToken"]?.toString() == "true",
            solicitanteId = solicitanteId,
            requestId = aggregateId,
            estadoNovo = estadoNovo,
        )

        val rendered =
            templateEngine.render(
                codigo = payload["notifyTemplate"]?.toString()?.takeIf { it.isNotBlank() }
                    ?: "solicitacoes.transicionada",
                vars =
                    mapOf(
                        "nome" to usuario.nome.escapeHtml(),
                        "tipo" to tipo.escapeHtml(),
                        "estadoNovo" to estadoNovo.escapeHtml(),
                        "parecerHtml" to parecerHtml,
                        "deepLinkHtml" to deepLinkHtml,
                    ),
                fallbackAssunto = "Atualização na sua solicitação — SecretariaOnline",
                fallbackCorpo =
                    """
                    <html><body>
                    <h2>Sua solicitação foi atualizada</h2>
                    <p>Olá, <strong>{{nome}}</strong>!</p>
                    <p>A solicitação <strong>{{tipo}}</strong> mudou para o estado <strong>{{estadoNovo}}</strong>.</p>
                    {{parecerHtml}}
                    {{deepLinkHtml}}
                    <p>Acesse o portal para ver os detalhes.</p>
                    <br><p>— Equipe SecretariaOnline UFPR</p>
                    </body></html>
                    """.trimIndent(),
            )

        mailService.sendNotificationEmail(
            to = usuario.email.value,
            subject = rendered.assunto,
            html = rendered.corpo,
        )
        inApp.deliver(solicitanteId, rendered.assunto, rendered.corpo)
    }

    private fun buildDeepLinkHtml(
        generateOneTimeToken: Boolean,
        solicitanteId: UUID,
        requestId: UUID,
        estadoNovo: String,
    ): String {
        if (!generateOneTimeToken) return ""
        return try {
            val token = oneTimeTokenPort.issueForRequest(solicitanteId, requestId)
            val url = "$frontendUrl/solicitacoes/$requestId?ott=$token"
            val label = if (estadoNovo == "EM_AJUSTE") "Acessar e reenviar solicitação →" else "Acessar solicitação diretamente →"
            "<p><a href='$url' style='display:inline-block;padding:10px 20px;background:#0057A8;color:#fff;border-radius:4px;text-decoration:none;font-weight:bold;'>$label</a></p>"
        } catch (e: Exception) {
            log.warn("Falha ao gerar one-time token para solicitação {}: {}", requestId, e.message)
            ""
        }
    }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
