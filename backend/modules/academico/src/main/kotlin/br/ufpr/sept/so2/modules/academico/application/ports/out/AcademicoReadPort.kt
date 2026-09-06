package br.ufpr.sept.so2.modules.academico.application.ports.out

import java.time.LocalDate
import java.util.UUID

data class CursoSearchHit(
    val id: UUID,
    val nome: String,
    val sigla: String,
)

data class PeriodoWindow(
    val inicio: LocalDate,
    val fim: LocalDate,
)

interface AcademicoReadPort {
    fun findCursoIdBySigla(sigla: String): UUID?

    fun findSigla(cursoId: UUID): String?

    fun findPeriodoWindow(
        ano: Short,
        semestre: Short,
    ): PeriodoWindow?

    fun searchCursos(q: String): List<CursoSearchHit>
}
