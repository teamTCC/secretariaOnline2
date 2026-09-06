package br.ufpr.sept.so2.modules.estagio.infrastructure.outbox

import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.MailService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EstagioOutboxHandler(
    private val usuarioRepository: UsuarioRepository,
    private val mailService: MailService,
) : OutboxEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(eventType: String): Boolean =
        eventType == OutboxEventTypes.ESTAGIO_DECLARADO ||
            eventType == OutboxEventTypes.ESTAGIO_CONCLUIDO ||
            eventType == OutboxEventTypes.ESTAGIO_SUPERVISOR_ATRIBUIDO

    override fun handle(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        val idAluno =
            payload["idAluno"]?.toString()?.let(UUID::fromString)
                ?: run {
                    log.warn("EstagioOutboxHandler: payload.idAluno ausente no outbox {}", aggregateId)
                    return
                }

        val usuario = usuarioRepository.findById(idAluno)
        if (usuario == null) {
            log.warn("EstagioOutboxHandler: usuário não encontrado idAluno={}", idAluno)
            return
        }

        val empresa = payload["empresa"]?.toString().orEmpty()
        val cargo = payload["cargo"]?.toString().orEmpty()

        when (eventType) {
            OutboxEventTypes.ESTAGIO_DECLARADO ->
                handleDeclarado(usuario.email.value, usuario.nome, empresa, cargo)

            OutboxEventTypes.ESTAGIO_CONCLUIDO -> {
                val cargaHoraria = payload["cargaHoraria"]?.toString() ?: "0"
                handleConcluido(usuario.email.value, usuario.nome, empresa, cargaHoraria)
            }

            OutboxEventTypes.ESTAGIO_SUPERVISOR_ATRIBUIDO -> {
                val supervisorNome = payload["supervisorNome"]?.toString().orEmpty()
                handleSupervisorAtribuido(usuario.email.value, usuario.nome, empresa, supervisorNome)
            }
        }
    }

    private fun handleDeclarado(
        email: String,
        nome: String,
        empresa: String,
        cargo: String,
    ) {
        mailService.sendNotificationEmail(
            to = email,
            subject = "Estágio declarado — ${empresa.escapeHtml()}",
            html =
                """
                <html><body>
                <h2>Estágio declarado com sucesso</h2>
                <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                <p>Estágio declarado com sucesso em <strong>${empresa.escapeHtml()}</strong> como <strong>${cargo.escapeHtml()}</strong>.</p>
                <br><p>— Equipe SecretariaOnline UFPR</p>
                </body></html>
                """.trimIndent(),
        )
    }

    private fun handleConcluido(
        email: String,
        nome: String,
        empresa: String,
        cargaHoraria: String,
    ) {
        mailService.sendNotificationEmail(
            to = email,
            subject = "Estágio concluído — ${empresa.escapeHtml()}",
            html =
                """
                <html><body>
                <h2>Estágio concluído</h2>
                <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                <p>Estágio concluído em <strong>${empresa.escapeHtml()}</strong>. CH registrada: <strong>${cargaHoraria}h</strong>.</p>
                <br><p>— Equipe SecretariaOnline UFPR</p>
                </body></html>
                """.trimIndent(),
        )
    }

    private fun handleSupervisorAtribuido(
        email: String,
        nome: String,
        empresa: String,
        supervisorNome: String,
    ) {
        mailService.sendNotificationEmail(
            to = email,
            subject = "Supervisor atribuído ao seu estágio — ${empresa.escapeHtml()}",
            html =
                """
                <html><body>
                <h2>Supervisor atribuído</h2>
                <p>Olá, <strong>${nome.escapeHtml()}</strong>!</p>
                <p>Supervisor <strong>${supervisorNome.escapeHtml()}</strong> atribuído ao seu estágio em <strong>${empresa.escapeHtml()}</strong>.</p>
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
