package br.ufpr.sept.so2.shared.outbox

import java.util.UUID

/**
 * Port for publishing outbox events. Decouples producers (formativas, estagio, etc.)
 * from the notificacoes infrastructure. The IAM module provides the implementation
 * via [OutboxEventPublisherImpl] backed by [OutboxEventJpaRepository].
 */
interface OutboxEventPublisher {
    fun enqueue(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any>,
    )
}
