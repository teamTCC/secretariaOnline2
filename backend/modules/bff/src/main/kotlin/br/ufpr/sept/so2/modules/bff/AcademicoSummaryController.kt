package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.AcademicoSummaryQuery
import br.ufpr.sept.so2.modules.bff.dto.AcademicoSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/academico")
@Tag(name = "BFF — Sumário Acadêmico", description = "Contagens agregadas do curso (Coordenador e Secretaria)")
class AcademicoSummaryController(
    private val query: AcademicoSummaryQuery,
) {
    @GetMapping("/relatorios/curso")
    @PreAuthorize("hasAuthority('dashboard.view_secretary')")
    @Operation(summary = "Relatório agregado do curso — alunos, TCC, estágios e solicitações")
    fun relatorioCurso(): AcademicoSummaryResponse = query.execute()
}
