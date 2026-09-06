package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.FaqCreatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.FaqUpdatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.TicketCreatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.TicketStateResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FaqItemEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FaqItemJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SupportTicketEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SupportTicketJpaRepository
import br.ufpr.sept.so2.shared.security.AuthenticatedUser
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class CreateFaqItemCommand(
    val categoria: String,
    val pergunta: String,
    val resposta: String,
    val ordem: Int,
)

data class UpdateFaqItemCommand(
    val id: UUID,
    val pergunta: String?,
    val resposta: String?,
    val ordem: Int?,
    val ativo: Boolean?,
)

data class CreateTicketCommand(
    val userId: UUID,
    val assunto: String,
    val descricao: String,
)

@Service
@Transactional
class ManageSupportUseCase(
    private val faqRepo: FaqItemJpaRepository,
    private val ticketRepo: SupportTicketJpaRepository,
) {
    fun createFaqItem(cmd: CreateFaqItemCommand): FaqCreatedResponse {
        val saved =
            faqRepo.save(
                FaqItemEntity(
                    categoria = cmd.categoria,
                    pergunta = cmd.pergunta,
                    resposta = cmd.resposta,
                    ordem = cmd.ordem,
                ),
            )
        return FaqCreatedResponse(
            id = saved.id,
            categoria = saved.categoria,
            pergunta = saved.pergunta,
            ordem = saved.ordem,
        )
    }

    fun updateFaqItem(cmd: UpdateFaqItemCommand): FaqUpdatedResponse {
        val item = faqRepo.findById(cmd.id).orElseThrow { NoSuchElementException("FAQ item não encontrado: ${cmd.id}") }
        cmd.pergunta?.let { item.pergunta = it }
        cmd.resposta?.let { item.resposta = it }
        cmd.ordem?.let { item.ordem = it }
        cmd.ativo?.let { item.ativo = it }
        val saved = faqRepo.save(item)
        return FaqUpdatedResponse(
            id = saved.id,
            categoria = saved.categoria,
            pergunta = saved.pergunta,
            resposta = saved.resposta,
            ordem = saved.ordem,
            ativo = saved.ativo,
        )
    }

    fun deleteFaqItem(id: UUID) {
        val item = faqRepo.findById(id).orElseThrow { NoSuchElementException("FAQ item não encontrado: $id") }
        item.ativo = false
        faqRepo.save(item)
    }

    fun createTicket(cmd: CreateTicketCommand): TicketCreatedResponse {
        val saved =
            ticketRepo.save(
                SupportTicketEntity(
                    idUsuario = cmd.userId,
                    assunto = cmd.assunto,
                    descricao = cmd.descricao,
                ),
            )
        return TicketCreatedResponse(id = saved.id, estado = saved.estado, assunto = saved.assunto)
    }

    fun respond(
        id: UUID,
        resposta: String,
        atendenteId: UUID,
    ): TicketStateResponse {
        val ticket = ticketRepo.findById(id).orElseThrow { NoSuchElementException("Ticket não encontrado: $id") }
        require(ticket.estado != "FECHADO") { "Ticket já está fechado." }
        ticket.resposta = resposta
        ticket.estado = "RESPONDIDO"
        ticket.idAtendente = atendenteId
        ticketRepo.save(ticket)
        return TicketStateResponse(id = ticket.id, estado = ticket.estado)
    }

    fun close(
        id: UUID,
        user: AuthenticatedUser,
    ): TicketStateResponse {
        val ticket = ticketRepo.findById(id).orElseThrow { NoSuchElementException("Ticket não encontrado: $id") }
        require(ticket.estado != "FECHADO") { "Ticket já está fechado." }
        val isOwner = ticket.idUsuario == user.userId
        val isStaff = user.authorities.contains("user.manage_students") || user.authorities.contains("system.admin")
        if (!isOwner && !isStaff) {
            throw AccessDeniedException("Apenas o autor do ticket ou a secretaria podem fechá-lo.")
        }
        ticket.estado = "FECHADO"
        ticketRepo.save(ticket)
        return TicketStateResponse(id = ticket.id, estado = ticket.estado)
    }
}
