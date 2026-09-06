package br.ufpr.sept.so2.modules.formativas.application

import br.ufpr.sept.so2.modules.formativas.api.dto.CaafStatsResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaPendenteResponse
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class CommissionsCaafQuery(
    private val activityRepo: FormativeActivityJpaRepository,
) {
    fun pool(pageable: Pageable): PageResponse<FormativaPendenteResponse> =
        PageResponse.ofWithLinks(activityRepo.findAllByEstadoAndIdRevisorIsNull("PENDENTE", pageable)) { a ->
            FormativaPendenteResponse(
                id = a.id,
                idAluno = a.idAluno,
                titulo = a.titulo,
                categoria = a.categoria,
                cargaHoraria = a.cargaHoraria,
                dataRealizacao = a.dataRealizacao,
            )
        }

    fun stats(): CaafStatsResponse {
        val startOfDay = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC)
        val totalPendente = activityRepo.countByEstado("PENDENTE")
        val aprovadasHoje = activityRepo.countByEstadoAndUpdatedAtAfter("APROVADA", startOfDay)
        val rejeitadasHoje = activityRepo.countByEstadoAndUpdatedAtAfter("REJEITADA", startOfDay)
        return CaafStatsResponse(
            totalPendente = totalPendente,
            aprovadasHoje = aprovadasHoje,
            rejeitadasHoje = rejeitadasHoje,
        )
    }
}
