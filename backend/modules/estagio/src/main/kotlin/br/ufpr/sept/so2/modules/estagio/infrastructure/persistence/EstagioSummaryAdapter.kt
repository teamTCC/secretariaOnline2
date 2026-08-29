package br.ufpr.sept.so2.modules.estagio.infrastructure.persistence

import br.ufpr.sept.so2.modules.estagio.application.ports.out.EstagioSummaryPort
import org.springframework.stereotype.Component

@Component
class EstagioSummaryAdapter(
    private val internshipRepo: InternshipJpaRepository,
) : EstagioSummaryPort {
    override fun countByEstado(estado: String): Long =
        internshipRepo.countByEstado(estado)

    override fun countSemSupervisor(): Long =
        internshipRepo.countByIdSupervisorIsNull()
}
