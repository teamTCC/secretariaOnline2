package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.FaqItemResponse
import br.ufpr.sept.so2.modules.iam.api.dto.TicketAdminSummaryResponse
import br.ufpr.sept.so2.modules.iam.api.dto.TicketSummaryResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FaqItemJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SupportTicketJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SupportQuery(
    private val faqRepo: FaqItemJpaRepository,
    private val ticketRepo: SupportTicketJpaRepository,
) {
    fun listFaq(categoria: String?): List<FaqItemResponse> {
        val items =
            if (categoria != null) {
                faqRepo.findAllByCategoriaAndAtivoOrderByOrdemAsc(categoria, true)
            } else {
                faqRepo.findAllByAtivoOrderByOrdemAsc(true)
            }
        return items.map { f ->
            FaqItemResponse(
                id = f.id,
                categoria = f.categoria,
                pergunta = f.pergunta,
                resposta = f.resposta,
                ordem = f.ordem,
            )
        }
    }

    fun myTickets(
        userId: UUID,
        pageable: Pageable,
    ): PageResponse<TicketSummaryResponse> =
        PageResponse.ofWithLinks(ticketRepo.findAllByIdUsuario(userId, pageable)) { t ->
            TicketSummaryResponse(
                id = t.id,
                assunto = t.assunto,
                estado = t.estado,
                resposta = t.resposta,
                createdAt = t.createdAt,
            )
        }

    fun listAll(
        estado: String?,
        pageable: Pageable,
    ): PageResponse<TicketAdminSummaryResponse> {
        val page =
            if (estado != null) {
                ticketRepo.findAllByEstado(estado.uppercase(), pageable)
            } else {
                ticketRepo.findAll(pageable)
            }
        return PageResponse.ofWithLinks(page) { t ->
            TicketAdminSummaryResponse(
                id = t.id,
                idUsuario = t.idUsuario,
                assunto = t.assunto,
                estado = t.estado,
                idAtendente = t.idAtendente,
                createdAt = t.createdAt,
            )
        }
    }
}
