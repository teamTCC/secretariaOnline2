package br.ufpr.sept.so2.modules.academico.infrastructure.persistence

import br.ufpr.sept.so2.modules.academico.application.ports.out.AcademicoReadPort
import br.ufpr.sept.so2.modules.academico.application.ports.out.CursoSearchHit
import br.ufpr.sept.so2.modules.academico.application.ports.out.PeriodoWindow
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AcademicoReadAdapter(
    private val cursoRepo: CursoJpaRepository,
    private val periodoRepo: PeriodoLetivoJpaRepository,
) : AcademicoReadPort {
    override fun findCursoIdBySigla(sigla: String): UUID? =
        cursoRepo.findBySigla(sigla.uppercase()).map { it.id }.orElse(null)

    override fun findSigla(cursoId: UUID): String? =
        cursoRepo.findById(cursoId).map { it.sigla }.orElse(null)

    override fun findPeriodoWindow(
        ano: Short,
        semestre: Short,
    ): PeriodoWindow? =
        periodoRepo.findByAnoAndSemestre(ano, semestre).map { p ->
            PeriodoWindow(inicio = p.inicio, fim = p.fim)
        }.orElse(null)

    override fun searchCursos(q: String): List<CursoSearchHit> =
        cursoRepo.findAllByAtivoTrue()
            .filter { c ->
                c.nome.contains(q, ignoreCase = true) || c.sigla.contains(q, ignoreCase = true)
            }
            .map { c -> CursoSearchHit(id = c.id, nome = c.nome, sigla = c.sigla) }
}
