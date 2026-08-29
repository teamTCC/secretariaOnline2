package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.ReportsQuery
import br.ufpr.sept.so2.modules.bff.dto.CoordinatorReportResponse
import br.ufpr.sept.so2.modules.bff.dto.SecretaryReportResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/reports")
@Tag(name = "BFF — Relatórios Analíticos", description = "Estatísticas da secretaria e relatórios analíticos de coordenação")
class ReportsController(
    private val reportsQuery: ReportsQuery,
) {
    @GetMapping("/secretary")
    @PreAuthorize("hasAuthority('report.view_secretary') or hasAuthority('system.admin')")
    @Operation(summary = "Estatísticas da secretaria — 4 datasets agregados")
    fun secretary(
        @RequestParam(required = false) periodo: String?,
        @RequestParam(required = false) curso: String?,
    ): SecretaryReportResponse = reportsQuery.secretary(periodo, curso)

    @GetMapping("/coordinator")
    @PreAuthorize("hasAuthority('report.view_coordinator') or hasAuthority('system.admin')")
    @Operation(summary = "Relatório analítico de coordenação")
    fun coordinator(
        @RequestParam(required = false) periodo: String?,
        @RequestParam(required = false) curso: String?,
    ): CoordinatorReportResponse = reportsQuery.coordinator(periodo, curso)
}
