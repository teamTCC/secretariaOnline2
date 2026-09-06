package br.ufpr.sept.so2.modules.notificacoes.application

import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class OutboxAdminUseCase(
    private val outboxRepo: OutboxEventJpaRepository,
) {
    fun retryEvent(id: UUID): String {
        val event = outboxRepo.findById(id).orElseThrow { NoSuchElementException("Evento outbox não encontrado: $id") }
        require(event.status == "DEAD") { "Somente eventos DEAD podem ser reenviados. Status atual: ${event.status}" }
        event.status = "PENDING"
        event.attemptCount = 0
        event.lastError = null
        event.nextAttemptAt = OffsetDateTime.now()
        outboxRepo.save(event)
        return event.status
    }

    fun discardEvent(id: UUID) {
        val event = outboxRepo.findById(id).orElseThrow { NoSuchElementException("Evento outbox não encontrado: $id") }
        require(event.status == "DEAD") { "Somente eventos DEAD podem ser descartados. Status atual: ${event.status}" }
        outboxRepo.delete(event)
    }
}
