package br.ufpr.sept.so2.modules.comunicacao.infrastructure.outbox

import br.ufpr.sept.so2.modules.comunicacao.application.InAppNotificationService
import br.ufpr.sept.so2.modules.comunicacao.application.TemplateEngine
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ComunicacaoOpsOutboxHandler(
    private val usuarioRepository: UsuarioRepository,
    private val mailService: MailService,
    private val templateEngine: TemplateEngine,
    private val inApp: InAppNotificationService,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean =
        eventType == OutboxEventTypes.ATENDIMENTO_CRIADO ||
            eventType == OutboxEventTypes.GRADUATION_CONFIRMED ||
            eventType == OutboxEventTypes.IMPORTS_COMPLETED ||
            eventType == OutboxEventTypes.EXPORTS_READY

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val usuarioId =
            payload["alunoId"]?.toString()?.let(UUID::fromString)
                ?: payload["atorId"]?.toString()?.let(UUID::fromString)
        if (usuarioId == null) {
            log.warn("ComunicacaoOpsOutboxHandler: destinatário ausente em {} {}", eventType, aggregateId)
            return
        }
        val usuario = usuarioRepository.findById(usuarioId)
        if (usuario == null || !usuario.ativo) {
            log.warn("ComunicacaoOpsOutboxHandler: usuário {} inativo/inexistente", usuarioId)
            return
        }

        val (codigo, fallbackAssunto, fallbackCorpo) = catalogFor(eventType)
        val rendered =
            templateEngine.render(
                codigo = codigo,
                vars =
                    mapOf(
                        "nome" to usuario.nome.escapeHtml(),
                        "assunto" to payload["assunto"]?.toString().orEmpty().escapeHtml(),
                        "status" to payload["status"]?.toString().orEmpty().escapeHtml(),
                        "kind" to payload["kind"]?.toString().orEmpty().escapeHtml(),
                    ),
                fallbackAssunto = fallbackAssunto,
                fallbackCorpo = fallbackCorpo,
            )
        mailService.sendNotificationEmail(to = usuario.email.value, subject = rendered.assunto, html = rendered.corpo)
        inApp.deliver(usuarioId, rendered.assunto, rendered.corpo)
    }

    private fun catalogFor(eventType: String): Triple<String, String, String> =
        when (eventType) {
            OutboxEventTypes.ATENDIMENTO_CRIADO ->
                Triple(
                    "atendimentos.created",
                    "Novo atendimento registrado",
                    "<html><body><h2>Atendimento registrado</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>A secretaria registrou um atendimento: <strong>{{assunto}}</strong>.</p></body></html>",
                )
            OutboxEventTypes.GRADUATION_CONFIRMED ->
                Triple(
                    "graduations.confirmed",
                    "Colação de grau confirmada",
                    "<html><body><h2>Colação de grau</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>Sua colação de grau foi confirmada.</p></body></html>",
                )
            OutboxEventTypes.IMPORTS_COMPLETED ->
                Triple(
                    "imports.completed",
                    "Importação CSV concluída",
                    "<html><body><h2>Importação concluída</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>Status <strong>{{status}}</strong>.</p></body></html>",
                )
            else ->
                Triple(
                    "exports.ready",
                    "Exportação pronta para download",
                    "<html><body><h2>Exportação pronta</h2><p>Olá, <strong>{{nome}}</strong>!</p><p>O arquivo <strong>{{kind}}</strong> está disponível.</p></body></html>",
                )
        }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
