package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.api.dto.CursoDetailResponse
import br.ufpr.sept.so2.modules.academico.api.dto.PeriodoLetivoSummaryResponse
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CoordenacaoQuery(
    private val cursoRepo: CursoJpaRepository,
    private val periodoRepo: PeriodoLetivoJpaRepository,
) {
    fun getCurso(id: UUID): CursoDetailResponse {
        val c = cursoRepo.findById(id).orElseThrow { NoSuchElementException("Curso não encontrado: $id") }
        return CursoDetailResponse(
            id = c.id,
            nome = c.nome,
            sigla = c.sigla,
            idCoordenador = c.idCoordenador,
            ativo = c.ativo,
            horasFormativasMinimas = c.horasFormativasMinimas,
            duracaoCalendario = c.duracaoCalendario,
            bancaMembrosExternos = c.bancaMembrosExternos,
            bancaModalidade = c.bancaModalidade,
            links = mapOf("config" to "/courses/${c.id}/config"),
        )
    }

    fun listPeriodos(): List<PeriodoLetivoSummaryResponse> =
        periodoRepo.findAll().map { p ->
            PeriodoLetivoSummaryResponse(
                id = p.id,
                ano = p.ano,
                semestre = p.semestre,
                inicio = p.inicio,
                fim = p.fim,
                ativo = p.ativo,
            )
        }
}
