package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.api.dto.CalendarioItemResponse
import br.ufpr.sept.so2.modules.academico.api.dto.CursoSummaryResponse
import br.ufpr.sept.so2.modules.academico.api.dto.DisciplinaSummaryResponse
import br.ufpr.sept.so2.modules.academico.api.dto.PeriodoAtivoResponse
import br.ufpr.sept.so2.modules.academico.application.AcademicoQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/academico")
@Tag(name = "Acadêmico", description = "Cursos, disciplinas e períodos letivos")
class AcademicoController(
    private val academicoQuery: AcademicoQuery,
) {
    @GetMapping("/cursos")
    @Operation(summary = "Listar cursos ativos")
    fun listCursos(): List<CursoSummaryResponse> = academicoQuery.listCursos()

    @GetMapping("/cursos/{cursoId}/disciplinas")
    @Operation(summary = "Listar disciplinas de um curso")
    fun listDisciplinas(
        @PathVariable cursoId: UUID,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 50) pageable: Pageable,
    ): PageResponse<DisciplinaSummaryResponse> = academicoQuery.listDisciplinas(cursoId, search, pageable)

    /**
     * Alias for form_schema `x-ui.endpoint: /academico/disciplinas`.
     * Optional [idCurso] filters; unused [enrolled]/[tipo] are accepted so seed URLs do not 400.
     */
    @GetMapping("/disciplinas")
    @Operation(summary = "Listar disciplinas ativas (filtro opcional por curso)")
    fun listDisciplinasAlias(
        @RequestParam(required = false) idCurso: UUID?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) enrolled: Boolean?,
        @RequestParam(required = false) tipo: String?,
        @PageableDefault(size = 50) pageable: Pageable,
    ): PageResponse<DisciplinaSummaryResponse> = academicoQuery.listDisciplinasAlias(idCurso, search, pageable)

    @GetMapping("/periodos/ativo")
    @Operation(summary = "Período letivo ativo atual")
    fun periodoAtivo(): PeriodoAtivoResponse = academicoQuery.periodoAtivo()

    @GetMapping("/periodos/{periodoId}/calendario")
    @Operation(summary = "Calendário acadêmico de um período")
    @PreAuthorize("isAuthenticated()")
    fun calendario(
        @PathVariable periodoId: UUID,
    ): List<CalendarioItemResponse> = academicoQuery.calendario(periodoId)
}
