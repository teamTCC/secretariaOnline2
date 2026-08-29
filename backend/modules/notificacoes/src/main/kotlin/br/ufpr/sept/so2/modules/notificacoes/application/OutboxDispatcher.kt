package br.ufpr.sept.so2.modules.notificacoes.application

import br.ufpr.sept.so2.modules.notificacoes.OutboxEventHandler
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.NotificationLogEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.NotificationLogJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class OutboxDispatcher(
    private val outboxRepo: OutboxEventJpaRepository,
    private val notificationLogRepo: NotificationLogJpaRepository,
    private val handlers: List<OutboxEventHandler>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun dispatch() {
        val now = OffsetDateTime.now()
        val pending = outboxRepo.findPendingEvents(now, PageRequest.of(0, BATCH_SIZE))

        if (pending.isEmpty()) return

        log.debug("Processando {} eventos do outbox", pending.size)

        pending.forEach { event ->
            try {
                val handler =
                    handlers.firstOrNull { it.supports(event.eventType) }
                        ?: throw IllegalStateException("Nenhum handler registrado para ${event.eventType}")

                handler.handle(event.eventType, event.aggregateType, event.aggregateId, event.payload)

                event.status = "PROCESSED"
                event.processedAt = now
                event.lastError = null
                outboxRepo.save(event)

                val destinatario =
                    event.payload["alunoId"]?.toString()?.let(UUID::fromString)
                        ?: event.payload["atorId"]?.toString()?.let(UUID::fromString)
                        ?: event.payload["idSolicitante"]?.toString()?.let(UUID::fromString)
                notificationLogRepo.save(
                    NotificationLogEntity(
                        eventType = event.eventType,
                        aggregateId = event.aggregateId,
                        idUsuario = destinatario,
                        canal = "EMAIL",
                        status = "SENT",
                    ),
                )
            } catch (e: Exception) {
                event.attemptCount += 1
                event.lastError = (e.message ?: "Erro desconhecido").take(2000)
                if (event.attemptCount >= MAX_ATTEMPTS) {
                    event.status = "DEAD"
                    log.error(
                        "Evento {} marcado DEAD após {} tentativas: {}",
                        event.eventType,
                        event.attemptCount,
                        e.message,
                    )
                } else {
                    event.status = "PENDING"
                    event.nextAttemptAt = now.plusSeconds(backoffSeconds(event.attemptCount))
                    log.error(
                        "Falha ao processar evento {} (tentativa {}): {}",
                        event.eventType,
                        event.attemptCount,
                        e.message,
                    )
                }
                outboxRepo.save(event)
            }
        }
    }

    private fun backoffSeconds(attempts: Int): Long =
        when {
            attempts < 3 -> 30L
            attempts < 5 -> 300L
            else -> 3600L
        }

    companion object {
        const val BATCH_SIZE = 50
        const val MAX_ATTEMPTS = 8
    }
}
