package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.ContactInfoLinks
import br.ufpr.sept.so2.modules.iam.api.dto.ContactInfoResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ContactMessageAcceptedResponse
import br.ufpr.sept.so2.modules.iam.config.ContatoProperties
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ContactMessageEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ContactMessageJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
class ContatoPublicoQuery(
    private val props: ContatoProperties,
) {
    fun get(): ContactInfoResponse =
        ContactInfoResponse(
            nome = props.nome,
            endereco = props.endereco,
            telefone = props.telefone,
            email = props.email,
            horario = props.horario,
            links = ContactInfoLinks(enviar = "/publico/contato"),
        )
}

data class SubmitContactCommand(
    val nome: String,
    val email: String,
    val assunto: String,
    val mensagem: String,
)

@Service
@Transactional
class SubmitContactUseCase(
    private val contactRepo: ContactMessageJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun execute(cmd: SubmitContactCommand): ContactMessageAcceptedResponse {
        val saved =
            contactRepo.save(
                ContactMessageEntity(
                    nome = cmd.nome.trim(),
                    email = cmd.email.trim().lowercase(),
                    assunto = cmd.assunto.trim(),
                    mensagem = cmd.mensagem.trim(),
                ),
            )
        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.CONTATO_RECEBIDO,
            aggregateType = "ContactMessage",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "nome" to saved.nome,
                    "email" to saved.email,
                    "assunto" to saved.assunto,
                    "mensagem" to saved.mensagem,
                ),
        )
        return ContactMessageAcceptedResponse(
            id = saved.id,
            status = "ACEITO",
            mensagem = "Mensagem recebida. Retornaremos por e-mail.",
        )
    }
}
