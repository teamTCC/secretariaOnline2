package br.ufpr.sept.so2.modules.formativas.infrastructure.persistence

import br.ufpr.sept.so2.modules.formativas.application.ports.out.FormativaBffReadPort
import br.ufpr.sept.so2.modules.formativas.application.ports.out.FormativaCategoriaCount
import br.ufpr.sept.so2.modules.formativas.application.ports.out.FormativaDashboardPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FormativaDashboardAdapter(
    private val formativeEntryRepo: FormativeEntryJpaRepository,
    private val activityRepo: FormativeActivityJpaRepository,
) : FormativaDashboardPort, FormativaBffReadPort {

    override fun sumHorasAprovadas(alunoId: UUID): Double =
        formativeEntryRepo.sumHorasAprovadas(alunoId)

    override fun countByEstado(estado: String): Long =
        activityRepo.countByEstado(estado)

    override fun countAprovadasByCategoria(): List<FormativaCategoriaCount> =
        activityRepo.countAprovadasByCategoria().map { row ->
            FormativaCategoriaCount(
                categoria = row[0].toString(),
                total = (row[1] as Number).toLong(),
            )
        }
}
