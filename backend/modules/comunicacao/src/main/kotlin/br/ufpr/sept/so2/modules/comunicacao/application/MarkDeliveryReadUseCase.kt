package br.ufpr.sept.so2.modules.comunicacao.application

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class MarkDeliveryReadCommand(
    val deliveryId: UUID,
    val userId: UUID,
)

@Service
@Transactional
class MarkDeliveryReadUseCase(
    private val deliveryRepo: CommunicationDeliveryJpaRepository,
) {
    fun execute(command: MarkDeliveryReadCommand) {
        val delivery =
            deliveryRepo
                .findById(command.deliveryId)
                .orElseThrow { NoSuchElementException("Delivery não encontrado: ${command.deliveryId}") }

        require(delivery.idUsuario == command.userId) { "Acesso negado." }

        deliveryRepo.markRead(command.deliveryId, OffsetDateTime.now())
    }
}
