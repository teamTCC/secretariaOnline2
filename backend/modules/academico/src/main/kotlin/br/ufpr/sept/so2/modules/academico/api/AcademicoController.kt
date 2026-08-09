package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CalendarioAcademicoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoJpaRepository
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
    private val cursoRepo: CursoJpaRepository,
    private val disciplinaRepo: DisciplinaJpaRepository,
    private val periodoRepo: PeriodoLetivoJpaRepository,
    private val calendarioRepo: CalendarioAcademicoJpaRepository,
) {
    @GetMapping("/cursos")
    @Operation(summary = "Listar cursos ativos")
    fun listCursos(): List<Map<String, Any?>> =
        cursoRepo.findAllByAtivoTrue().map { c ->
            mapOf("id" to c.id, "nome" to c.nome, "sigla" to c.sigla)
        }

    @GetMapping("/cursos/{cursoId}/disciplinas")
    @Operation(summary = "Listar disciplinas de um curso")
    fun listDisciplinas(
        @PathVariable cursoId: UUID,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 50) pageable: Pageable,
    ) = disciplinaRepo.searchByCurso(cursoId, search, pageable).map { d ->
        mapOf("id" to d.id, "codigo" to d.codigo, "nome" to d.nome, "creditos" to d.creditos)
    }

    @GetMapping("/periodos/ativo")
    @Operation(summary = "Período letivo ativo atual")
    fun periodoAtivo(): Map<String, Any?> {
        val periodo =
            periodoRepo
                .findFirstByAtivoTrueOrderByAnoDescSemestreDesc()
                .orElseThrow { NoSuchElementException("Nenhum período letivo ativo encontrado") }
        return mapOf(
            "id" to periodo.id,
            "ano" to periodo.ano,
            "semestre" to periodo.semestre,
            "inicio" to periodo.inicio,
            "fim" to periodo.fim,
        )
    }

    @GetMapping("/periodos/{periodoId}/calendario")
    @Operation(summary = "Calendário acadêmico de um período")
    @PreAuthorize("isAuthenticated()")
    fun calendario(
        @PathVariable periodoId: UUID,
    ) = calendarioRepo.findAllByIdPeriodoLetivo(periodoId).map { c ->
        mapOf("id" to c.id, "descricao" to c.descricao, "prazoInicio" to c.prazoInicio, "prazoFim" to c.prazoFim)
    }
}
