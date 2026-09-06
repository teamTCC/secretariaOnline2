package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.api.dto.CalendarioItemResponse
import br.ufpr.sept.so2.modules.academico.api.dto.CursoSummaryResponse
import br.ufpr.sept.so2.modules.academico.api.dto.DisciplinaSummaryResponse
import br.ufpr.sept.so2.modules.academico.api.dto.PeriodoAtivoResponse
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CalendarioAcademicoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AcademicoQuery(
    private val cursoRepo: CursoJpaRepository,
    private val disciplinaRepo: DisciplinaJpaRepository,
    private val periodoRepo: PeriodoLetivoJpaRepository,
    private val calendarioRepo: CalendarioAcademicoJpaRepository,
) {
    fun listCursos(): List<CursoSummaryResponse> =
        cursoRepo.findAllByAtivoTrue().map { c ->
            CursoSummaryResponse(id = c.id, nome = c.nome, sigla = c.sigla)
        }

    fun listDisciplinas(
        cursoId: UUID,
        search: String?,
        pageable: Pageable,
    ): PageResponse<DisciplinaSummaryResponse> =
        PageResponse.of(disciplinaRepo.searchByCurso(cursoId, search, pageable)) { d ->
            DisciplinaSummaryResponse(id = d.id, codigo = d.codigo, nome = d.nome, creditos = d.creditos)
        }

    fun listDisciplinasAlias(
        idCurso: UUID?,
        search: String?,
        pageable: Pageable,
    ): PageResponse<DisciplinaSummaryResponse> =
        PageResponse.of(disciplinaRepo.searchActive(idCurso, search, pageable)) { d ->
            DisciplinaSummaryResponse(id = d.id, codigo = d.codigo, nome = d.nome, creditos = d.creditos)
        }

    fun periodoAtivo(): PeriodoAtivoResponse {
        val periodo =
            periodoRepo
                .findFirstByAtivoTrueOrderByAnoDescSemestreDesc()
                .orElseThrow { NoSuchElementException("Nenhum período letivo ativo encontrado") }
        return PeriodoAtivoResponse(
            id = periodo.id,
            ano = periodo.ano,
            semestre = periodo.semestre,
            inicio = periodo.inicio,
            fim = periodo.fim,
        )
    }

    fun calendario(periodoId: UUID): List<CalendarioItemResponse> =
        calendarioRepo.findAllByIdPeriodoLetivo(periodoId).map { c ->
            CalendarioItemResponse(
                id = c.id,
                descricao = c.descricao,
                prazoInicio = c.prazoInicio,
                prazoFim = c.prazoFim,
            )
        }
}
