package br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence

import br.ufpr.sept.so2.modules.comunicacao.application.ports.out.ComunicacaoDashboardPort
import br.ufpr.sept.so2.modules.comunicacao.application.ports.out.ComunicadoCardDto
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ComunicacaoDashboardAdapter(
    private val deliveryRepo: CommunicationDeliveryJpaRepository,
) : ComunicacaoDashboardPort {

    override fun findRecentesByUsuario(usuarioId: UUID, limit: Int): List<ComunicadoCardDto> =
        deliveryRepo
            .findAllByIdUsuarioOrderByDeliveredAtDesc(usuarioId, PageRequest.of(0, limit))
            .content.map { d ->
                ComunicadoCardDto(
                    id = d.id,
                    idCommunication = d.idCommunication,
                    deliveredAt = d.deliveredAt,
                    readAt = d.readAt,
                )
            }
}
