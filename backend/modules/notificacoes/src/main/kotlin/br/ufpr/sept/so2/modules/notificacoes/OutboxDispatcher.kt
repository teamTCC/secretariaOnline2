package br.ufpr.sept.so2.modules.notificacoes

import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class OutboxDispatcher(
    private val outboxRepo: OutboxEventJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun dispatch() {
        val now = OffsetDateTime.now()
        val pending = outboxRepo.findPendingEvents(now, PageRequest.of(0, 50))

        if (pending.isEmpty()) return

        log.debug("Processando {} eventos do outbox", pending.size)

        pending.forEach { event ->
            try {
                processEvent(event.eventType, event.aggregateType, event.aggregateId, event.payload)
                outboxRepo.markProcessed(event.id, now)
            } catch (e: Exception) {
                val nextAttempt = now.plusSeconds(backoffSeconds(event.attemptCount))
                outboxRepo.markFailed(event.id, e.message ?: "Erro desconhecido", nextAttempt)
                log.error("Falha ao processar evento {} (tentativa {}): {}", event.eventType, event.attemptCount + 1, e.message)
            }
        }
    }

    private fun processEvent(
        eventType: String,
        aggregateType: String,
        aggregateId: java.util.UUID,
        payload: Map<String, Any>,
    ) {
        // Dispatching logic — each event type is handled by a specific processor
        // In MVP, most events trigger email notifications
        log.info("Processando evento: {} para {}:{}", eventType, aggregateType, aggregateId)
        // TODO: route to specific handlers (email, push, audit) based on eventType
    }

    private fun backoffSeconds(attempts: Int): Long =
        when {
            attempts < 3 -> 30L
            attempts < 5 -> 300L
            else -> 3600L
        }
}
