package br.ufpr.sept.so2.modules.iam.infrastructure.outbox

import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Persists outbox events inside the caller's active transaction (Propagation.REQUIRED).
 * Any module that depends on :shared can inject this port without knowing notificacoes.
 */
@Service
class OutboxEventPublisherImpl(
    private val outboxRepo: OutboxEventJpaRepository,
) : OutboxEventPublisher {
    override fun enqueue(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    ) {
        outboxRepo.save(
            OutboxEventEntity(
                eventType = eventType,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                payload = payload,
            ),
        )
    }
}
