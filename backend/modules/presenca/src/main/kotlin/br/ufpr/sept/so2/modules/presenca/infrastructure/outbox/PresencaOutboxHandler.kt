package br.ufpr.sept.so2.modules.presenca.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PresencaOutboxHandler(
    private val usuarioRepository: UsuarioRepository,
    private val mailService: MailService,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean =
        eventType == OutboxEventTypes.PRESENCA_CONFIRMADA ||
            eventType == OutboxEventTypes.CERTIFICATE_ISSUED

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        if (eventType == OutboxEventTypes.CERTIFICATE_ISSUED) {
            handleCertificateIssued(payload, aggregateId)
            return
        }

        val alunoId =
            payload["alunoId"]?.toString()?.let(UUID::fromString)
                ?: run {
                    log.warn("PresencaOutboxHandler: payload.alunoId ausente no outbox {}", aggregateId)
                    return
                }

        val usuario = usuarioRepository.findById(alunoId)
        if (usuario == null) {
            log.warn("PresencaOutboxHandler: usuário não encontrado alunoId={}", alunoId)
            return
        }

        val eventoTitulo = payload["eventoTitulo"]?.toString().orEmpty()
        val phase = payload["phase"]?.toString() ?: "ENTRY"

        val (subject, html) =
            when (phase) {
                "ENTRY" ->
                    "Entrada confirmada no evento: ${eventoTitulo}" to
                        """
                        <html><body>
                        <h2>Presença confirmada</h2>
                        <p>Olá, <strong>${usuario.nome.escapeHtml()}</strong>!</p>
                        <p>Sua entrada foi confirmada no evento: <strong>${eventoTitulo.escapeHtml()}</strong>.</p>
                        <br><p>— Equipe SecretariaOnline UFPR</p>
                        </body></html>
                        """.trimIndent()

                "EXIT" -> {
                    val chCreditadas = payload["chCreditadas"]?.toString() ?: "0"
                    "Saída confirmada em ${eventoTitulo}" to
                        """
                        <html><body>
                        <h2>Saída confirmada</h2>
                        <p>Olá, <strong>${usuario.nome.escapeHtml()}</strong>!</p>
                        <p>Sua saída foi confirmada em <strong>${eventoTitulo.escapeHtml()}</strong>.</p>
                        <p><strong>${chCreditadas}h</strong> serão creditadas após a conclusão do evento.</p>
                        <br><p>— Equipe SecretariaOnline UFPR</p>
                        </body></html>
                        """.trimIndent()
                }

                else -> {
                    log.warn("PresencaOutboxHandler: phase desconhecida '{}' no outbox {}", phase, aggregateId)
                    return
                }
            }

        mailService.sendNotificationEmail(to = usuario.email.value, subject = subject, html = html)
    }

    private fun handleCertificateIssued(
        payload: Map<String, Any>,
        aggregateId: UUID,
    ) {
        val alunoId =
            payload["alunoId"]?.toString()?.let(UUID::fromString)
                ?: run {
                    log.warn("PresencaOutboxHandler: payload.alunoId ausente no certificado {}", aggregateId)
                    return
                }
        val usuario = usuarioRepository.findById(alunoId)
        if (usuario == null) {
            log.warn("PresencaOutboxHandler: usuário não encontrado alunoId={}", alunoId)
            return
        }
        val eventoTitulo = payload["eventoTitulo"]?.toString().orEmpty()
        val hash = payload["hashSha256"]?.toString().orEmpty()
        mailService.sendNotificationEmail(
            to = usuario.email.value,
            subject = "Certificado emitido: $eventoTitulo",
            html =
                """
                <html><body>
                <h2>Certificado disponível</h2>
                <p>Olá, <strong>${usuario.nome.escapeHtml()}</strong>!</p>
                <p>Seu certificado de participação em <strong>${eventoTitulo.escapeHtml()}</strong> foi emitido.</p>
                <p>Verifique em: /publico/verificar-certificado/${hash.escapeHtml()}</p>
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
