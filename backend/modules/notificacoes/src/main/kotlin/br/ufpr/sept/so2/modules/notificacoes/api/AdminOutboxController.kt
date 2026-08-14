package br.ufpr.sept.so2.modules.notificacoes.api

import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/admin/outbox")
@Tag(name = "Admin — Outbox", description = "Inspeção e reenvio manual de eventos do outbox (system.admin)")
@PreAuthorize("hasAuthority('system.admin')")
class AdminOutboxController(
    private val outboxRepo: OutboxEventJpaRepository,
) {
    @GetMapping
    @Operation(summary = "Listar eventos do outbox por status (PENDING, PROCESSED, DEAD)")
    fun list(
        @RequestParam(defaultValue = "PENDING") status: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(outboxRepo.findAllByStatusOrderByCreatedAtDesc(status.uppercase(), pageable)) { e ->
            mapOf(
                "id" to e.id,
                "eventType" to e.eventType,
                "aggregateType" to e.aggregateType,
                "aggregateId" to e.aggregateId,
                "status" to e.status,
                "attemptCount" to e.attemptCount,
                "nextAttemptAt" to e.nextAttemptAt,
                "processedAt" to e.processedAt,
                "createdAt" to e.createdAt,
            )
        }

    @GetMapping("/dead")
    @Operation(summary = "Listar apenas eventos DEAD (falharam após todas as tentativas)")
    fun listDead(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(outboxRepo.findAllByStatusOrderByCreatedAtDesc("DEAD", pageable)) { e ->
            mapOf(
                "id" to e.id,
                "eventType" to e.eventType,
                "aggregateId" to e.aggregateId,
                "attemptCount" to e.attemptCount,
                "lastError" to e.lastError,
                "createdAt" to e.createdAt,
            )
        }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um evento do outbox (inclui payload e lastError)")
    fun detail(
        @PathVariable id: UUID,
    ): Map<String, Any?> {
        val event = outboxRepo.findById(id).orElseThrow { NoSuchElementException("Evento outbox não encontrado: $id") }
        return mapOf(
            "id" to event.id,
            "eventType" to event.eventType,
            "aggregateType" to event.aggregateType,
            "aggregateId" to event.aggregateId,
            "payload" to event.payload,
            "status" to event.status,
            "attemptCount" to event.attemptCount,
            "lastError" to event.lastError,
            "nextAttemptAt" to event.nextAttemptAt,
            "processedAt" to event.processedAt,
            "createdAt" to event.createdAt,
            "updatedAt" to event.updatedAt,
        )
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Reenviar evento DEAD — redefine status para PENDING")
    @Transactional
    fun retry(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val event = outboxRepo.findById(id).orElseThrow { NoSuchElementException("Evento outbox não encontrado: $id") }
        require(event.status == "DEAD") { "Somente eventos DEAD podem ser reenviados. Status atual: ${event.status}" }

        event.status = "PENDING"
        event.attemptCount = 0
        event.lastError = null
        event.nextAttemptAt = OffsetDateTime.now()
        outboxRepo.save(event)

        return ResponseEntity.ok(mapOf("id" to event.id, "status" to event.status))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Descartar evento DEAD — remove permanentemente da fila")
    @Transactional
    fun discard(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val event = outboxRepo.findById(id).orElseThrow { NoSuchElementException("Evento outbox não encontrado: $id") }
        require(event.status == "DEAD") { "Somente eventos DEAD podem ser descartados. Status atual: ${event.status}" }
        outboxRepo.delete(event)
        return ResponseEntity.noContent().build()
    }
}
