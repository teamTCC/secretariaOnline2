package br.ufpr.sept.so2.modules.formativas.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FormativasOutboxHandler(
    private val usuarioRepository: UsuarioRepository,
    private val mailService: MailService,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean =
        eventType == OutboxEventTypes.FORMATIVA_REVISADA ||
            eventType == OutboxEventTypes.FORMATIVA_BATCH_REVISADA

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val idAluno =
            payload["idAluno"]?.toString()?.let(UUID::fromString)
                ?: run {
                    log.warn("FormativasOutboxHandler: payload.idAluno ausente no outbox {}", aggregateId)
                    return
                }

        val usuario = usuarioRepository.findById(idAluno)
        if (usuario == null) {
            log.warn("FormativasOutboxHandler: usuário não encontrado idAluno={}", idAluno)
            return
        }

        when (eventType) {
            OutboxEventTypes.FORMATIVA_REVISADA -> handleRevisada(usuario.email.value, usuario.nome, payload)
            OutboxEventTypes.FORMATIVA_BATCH_REVISADA -> handleBatchRevisada(usuario.email.value, usuario.nome, payload)
        }
    }

    private fun handleRevisada(
        email: String,
        nome: String,
        payload: Map<String, Any>,
    ) {
        val estado = payload["estado"]?.toString() ?: return
        val chCreditada = payload["chCreditada"]?.toString() ?: "0"
        val parecer = payload["parecer"]?.toString()

        val (subject, bodyHtml) =
            when (estado) {
                "APROVADA" ->
                    "Atividade formativa aprovada — ${chCreditada}h creditadas" to
                        """
                        <html><body>
                        <h2>Atividade formativa aprovada</h2>
                        <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                        <p>Sua atividade formativa foi <strong>aprovada</strong>.</p>
                        <p><strong>${chCreditada}h</strong> foram creditadas à sua carga horária complementar.</p>
                        <br><p>— Equipe SecretariaOnline UFPR</p>
                        </body></html>
                        """.trimIndent()

                "REJEITADA" ->
                    "Atividade formativa rejeitada" to
                        """
                        <html><body>
                        <h2>Atividade formativa rejeitada</h2>
                        <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                        <p>Sua atividade formativa foi <strong>rejeitada</strong>.</p>
                        ${if (parecer != null) "<p><strong>Parecer:</strong> ${parecer.escapeHtml()}</p>" else ""}
                        <br><p>— Equipe SecretariaOnline UFPR</p>
                        </body></html>
                        """.trimIndent()

                else -> {
                    log.warn("FormativasOutboxHandler: estado desconhecido '{}' para FORMATIVA_REVISADA", estado)
                    return
                }
            }

        mailService.sendNotificationEmail(to = email, subject = subject, html = bodyHtml)
    }

    private fun handleBatchRevisada(
        email: String,
        nome: String,
        payload: Map<String, Any>,
    ) {
        val totalAprovadas = payload["totalAprovadas"]?.toString() ?: "0"
        val totalRejeitadas = payload["totalRejeitadas"]?.toString() ?: "0"
        val chAprovada = payload["chAprovada"]?.toString() ?: "0"

        mailService.sendNotificationEmail(
            to = email,
            subject = "Revisão em lote de atividades formativas concluída",
            html =
                """
                <html><body>
                <h2>Revisão em lote processada</h2>
                <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                <p>Revisão em lote processada: <strong>$totalAprovadas</strong> aprovadas, <strong>$totalRejeitadas</strong> rejeitadas. CH total: <strong>${chAprovada}h</strong>.</p>
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
