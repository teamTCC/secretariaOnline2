package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.api.dto.CommunicationDeliveryResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.CommunicationDetailResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.CommunicationSummaryResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.UnreadCountResponse
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CommunicationsQuery(
    private val communicationRepo: CommunicationJpaRepository,
    private val deliveryRepo: CommunicationDeliveryJpaRepository,
) {
    fun listPublished(pageable: Pageable): PageResponse<CommunicationSummaryResponse> =
        PageResponse.of(communicationRepo.findAllByPublishedAtIsNotNullOrderByPublishedAtDesc(pageable)) { c ->
            CommunicationSummaryResponse(
                id = c.id,
                titulo = c.titulo,
                tipo = c.tipo,
                publishedAt = c.publishedAt,
                audiencia = c.audiencia.ifEmpty { null },
            )
        }

    fun myInbox(
        userId: UUID,
        pageable: Pageable,
    ): PageResponse<CommunicationDeliveryResponse> =
        PageResponse.of(deliveryRepo.findAllByIdUsuarioOrderByDeliveredAtDesc(userId, pageable)) { d ->
            CommunicationDeliveryResponse(
                deliveryId = d.id,
                idCommunication = d.idCommunication,
                canal = d.canal,
                status = d.status,
                deliveredAt = d.deliveredAt,
                readAt = d.readAt,
                links =
                    buildMap {
                        put("self", "/communications/${d.idCommunication}")
                        if (d.readAt == null) {
                            put("read", "/communications/deliveries/${d.id}/read")
                        }
                    },
            )
        }

    fun unreadCount(userId: UUID): UnreadCountResponse =
        UnreadCountResponse(unread = deliveryRepo.countByIdUsuarioAndReadAtIsNull(userId))

    fun getById(id: UUID): CommunicationDetailResponse {
        val communication =
            communicationRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Comunicado não encontrado: $id") }

        require(communication.publishedAt != null) { "Comunicado não publicado." }

        return CommunicationDetailResponse(
            id = communication.id,
            titulo = communication.titulo,
            conteudo = communication.conteudo,
            tipo = communication.tipo,
            audiencia = communication.audiencia.ifEmpty { null },
            publishedAt = communication.publishedAt,
            expiresAt = communication.expiresAt,
        )
    }
}
