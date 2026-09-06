package br.ufpr.sept.so2.modules.tcc.infrastructure.persistence

import br.ufpr.sept.so2.modules.tcc.application.ports.out.TccDashboardPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TccDashboardAdapter(
    private val tccRepo: TccJpaRepository,
) : TccDashboardPort {
    override fun countDefendidosByAluno(alunoId: UUID): Int =
        tccRepo.findByAluno(alunoId).count { it.aprovado == true }

    override fun countByEstado(estado: String): Long =
        tccRepo.countByEstado(estado)
}
