package br.ufpr.sept.so2.modules.notificacoes.api

import br.ufpr.sept.so2.modules.notificacoes.api.dto.OutboxDeadEventResponse
import br.ufpr.sept.so2.modules.notificacoes.api.dto.OutboxEventDetailResponse
import br.ufpr.sept.so2.modules.notificacoes.api.dto.OutboxEventSummaryResponse
import br.ufpr.sept.so2.modules.notificacoes.api.dto.OutboxRetryResponse
import br.ufpr.sept.so2.modules.notificacoes.application.AdminOutboxQuery
import br.ufpr.sept.so2.modules.notificacoes.application.OutboxAdminUseCase
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/admin/outbox")
@Tag(name = "Admin — Outbox", description = "Inspeção e reenvio manual de eventos do outbox (system.admin)")
@PreAuthorize("hasAuthority('system.admin')")
class AdminOutboxController(
    private val adminOutboxQuery: AdminOutboxQuery,
    private val outboxAdminUseCase: OutboxAdminUseCase,
) {
    @GetMapping
    @Operation(summary = "Listar eventos do outbox por status (PENDING, PROCESSED, DEAD)")
    fun list(
        @RequestParam(defaultValue = "PENDING") status: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<OutboxEventSummaryResponse> = adminOutboxQuery.list(status, pageable)

    @GetMapping("/dead")
    @Operation(summary = "Listar apenas eventos DEAD (falharam após todas as tentativas)")
    fun listDead(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<OutboxDeadEventResponse> = adminOutboxQuery.listDead(pageable)

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um evento do outbox (inclui payload e lastError)")
    fun detail(
        @PathVariable id: UUID,
    ): OutboxEventDetailResponse = adminOutboxQuery.detail(id)

    @PostMapping("/{id}/retry")
    @Operation(summary = "Reenviar evento DEAD — redefine status para PENDING")
    fun retry(
        @PathVariable id: UUID,
    ): ResponseEntity<OutboxRetryResponse> {
        val newStatus = outboxAdminUseCase.retryEvent(id)
        return ResponseEntity.ok(OutboxRetryResponse(id = id, status = newStatus))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Descartar evento DEAD — remove permanentemente da fila")
    fun discard(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        outboxAdminUseCase.discardEvent(id)
        return ResponseEntity.noContent().build()
    }
}
