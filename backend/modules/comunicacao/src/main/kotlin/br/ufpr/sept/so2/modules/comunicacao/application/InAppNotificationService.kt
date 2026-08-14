package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationJpaRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class InAppNotificationService(
    private val communicationRepo: CommunicationJpaRepository,
    private val deliveryRepo: CommunicationDeliveryJpaRepository,
) {
    fun deliver(
        usuarioId: UUID,
        titulo: String,
        html: String,
    ) {
        val now = OffsetDateTime.now()
        val communication =
            communicationRepo.save(
                CommunicationEntity(
                    idAutor = usuarioId,
                    titulo = titulo.take(200),
                    conteudo = html,
                    tipo = "INFORMATIVO",
                    audiencia = mapOf("usuarioId" to usuarioId.toString()),
                    publishedAt = now,
                ),
            )
        deliveryRepo.save(
            CommunicationDeliveryEntity(
                idCommunication = communication.id,
                idUsuario = usuarioId,
                canal = "IN_APP",
                status = "ENTREGUE",
                deliveredAt = now,
            ),
        )
    }
}
