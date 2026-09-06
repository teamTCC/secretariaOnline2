package br.ufpr.sept.so2.modules.notificacoes.application

import br.ufpr.sept.so2.modules.notificacoes.api.dto.OutboxDeadEventResponse
import br.ufpr.sept.so2.modules.notificacoes.api.dto.OutboxEventDetailResponse
import br.ufpr.sept.so2.modules.notificacoes.api.dto.OutboxEventSummaryResponse
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AdminOutboxQuery(
    private val outboxRepo: OutboxEventJpaRepository,
) {
    fun list(
        status: String,
        pageable: Pageable,
    ): PageResponse<OutboxEventSummaryResponse> =
        PageResponse.ofWithLinks(outboxRepo.findAllByStatusOrderByCreatedAtDesc(status.uppercase(), pageable)) { e ->
            OutboxEventSummaryResponse(
                id = e.id,
                eventType = e.eventType,
                aggregateType = e.aggregateType,
                aggregateId = e.aggregateId,
                status = e.status,
                retryCount = e.attemptCount,
                createdAt = e.createdAt,
                sentAt = e.processedAt,
            )
        }

    fun listDead(pageable: Pageable): PageResponse<OutboxDeadEventResponse> =
        PageResponse.ofWithLinks(outboxRepo.findAllByStatusOrderByCreatedAtDesc("DEAD", pageable)) { e ->
            OutboxDeadEventResponse(
                id = e.id,
                eventType = e.eventType,
                aggregateId = e.aggregateId,
                retryCount = e.attemptCount,
                lastError = e.lastError,
                createdAt = e.createdAt,
            )
        }

    fun detail(id: UUID): OutboxEventDetailResponse {
        val event = outboxRepo.findById(id).orElseThrow { NoSuchElementException("Evento outbox não encontrado: $id") }
        return OutboxEventDetailResponse(
            id = event.id,
            eventType = event.eventType,
            aggregateType = event.aggregateType,
            aggregateId = event.aggregateId,
            payload = event.payload,
            status = event.status,
            retryCount = event.attemptCount,
            lastError = event.lastError,
            nextAttemptAt = event.nextAttemptAt,
            sentAt = event.processedAt,
            createdAt = event.createdAt,
            updatedAt = event.updatedAt,
        )
    }
}
