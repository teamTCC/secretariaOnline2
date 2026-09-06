package br.ufpr.sept.so2.modules.auditoria.api

import br.ufpr.sept.so2.modules.auditoria.api.dto.AuditLogSummaryResponse
import br.ufpr.sept.so2.modules.auditoria.application.AuditQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/admin/audit")
@Tag(name = "Admin — Auditoria", description = "Consulta ao log de auditoria imutável")
class AuditController(
    private val auditQuery: AuditQuery,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('audit.read')")
    @Operation(summary = "Consultar log de auditoria com filtros opcionais")
    fun query(
        @RequestParam(required = false) idAtor: UUID?,
        @RequestParam(required = false) acao: String?,
        @RequestParam(required = false) resultado: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        since: OffsetDateTime?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<AuditLogSummaryResponse> = auditQuery.query(idAtor, acao, resultado, since, pageable)
}
