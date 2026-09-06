package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.estagio.api.dto.CoePoolItemResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.CoeStatsResponse
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@Component
class CommissionsCoeQuery(
    private val internshipRepo: InternshipJpaRepository,
) {
    fun pool(pageable: Pageable): PageResponse<CoePoolItemResponse> =
        PageResponse.ofWithLinks(internshipRepo.findAllByEstadoAndIdSupervisorIsNull("EM_ANDAMENTO", pageable)) { i ->
            CoePoolItemResponse(
                id = i.id,
                idAluno = i.idAluno,
                empresa = i.empresa,
                cargo = i.cargo,
                cargaHorariaSemanal = i.cargaHorariaSemanal,
                inicio = i.inicio,
            )
        }

    fun stats(): CoeStatsResponse {
        val startOfMonth =
            OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)

        val semSupervisor = internshipRepo.countByIdSupervisorIsNull()
        val atribuidosEsteMes = internshipRepo.countBySupervisorAssignedAfter(startOfMonth)

        return CoeStatsResponse(
            semSupervisor = semSupervisor,
            atribuidosEsteMes = atribuidosEsteMes,
        )
    }
}
