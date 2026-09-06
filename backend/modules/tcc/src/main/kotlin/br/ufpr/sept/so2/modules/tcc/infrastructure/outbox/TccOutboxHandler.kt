package br.ufpr.sept.so2.modules.tcc.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TccOutboxHandler(
    private val usuarioRepository: UsuarioRepository,
    private val mailService: MailService,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean =
        eventType == OutboxEventTypes.TCC_CRIADO ||
            eventType == OutboxEventTypes.TCC_DELIBERADO

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val idAluno =
            payload["idAluno"]?.toString()?.let(UUID::fromString)
                ?: run {
                    log.warn("TccOutboxHandler: payload.idAluno ausente no outbox {}", aggregateId)
                    return
                }

        val usuario = usuarioRepository.findById(idAluno)
        if (usuario == null) {
            log.warn("TccOutboxHandler: usuário não encontrado idAluno={}", idAluno)
            return
        }

        val titulo = payload["titulo"]?.toString().orEmpty()

        when (eventType) {
            OutboxEventTypes.TCC_CRIADO ->
                handleCriado(usuario.email.value, usuario.nome, titulo)

            OutboxEventTypes.TCC_DELIBERADO -> {
                val estado = payload["estado"]?.toString() ?: return
                val parecer = payload["parecer"]?.toString()
                handleDeliberado(usuario.email.value, usuario.nome, titulo, estado, parecer)
            }
        }
    }

    private fun handleCriado(
        email: String,
        nome: String,
        titulo: String,
    ) {
        mailService.sendNotificationEmail(
            to = email,
            subject = "TCC registrado — ${titulo.escapeHtml()}",
            html =
                """
                <html><body>
                <h2>TCC registrado com sucesso</h2>
                <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                <p>TCC registrado: <strong>${titulo.escapeHtml()}</strong>. Aguarde avaliação da banca.</p>
                <br><p>— Equipe SecretariaOnline UFPR</p>
                </body></html>
                """.trimIndent(),
        )
    }

    private fun handleDeliberado(
        email: String,
        nome: String,
        titulo: String,
        estado: String,
        parecer: String?,
    ) {
        val estadoLabel =
            when (estado) {
                "APROVADO" -> "aprovado"
                "REPROVADO" -> "reprovado"
                else -> estado.lowercase()
            }

        val parecerHtml =
            if (!parecer.isNullOrBlank()) {
                "<p><strong>Parecer:</strong> ${parecer.escapeHtml()}</p>"
            } else {
                ""
            }

        mailService.sendNotificationEmail(
            to = email,
            subject = "Resultado do TCC: ${titulo.escapeHtml()} — $estadoLabel",
            html =
                """
                <html><body>
                <h2>Resultado do TCC</h2>
                <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                <p>TCC <strong>${titulo.escapeHtml()}</strong> foi <strong>$estadoLabel</strong>.</p>
                $parecerHtml
                <br><p>— Equipe SecretariaOnline UFPR</p>
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
