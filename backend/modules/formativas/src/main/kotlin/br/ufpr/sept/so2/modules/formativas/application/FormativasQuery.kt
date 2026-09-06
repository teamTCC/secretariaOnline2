package br.ufpr.sept.so2.modules.formativas.application

import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaPendenteResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaResumoResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaSummaryResponse
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FormativasQuery(
    private val activityRepo: FormativeActivityJpaRepository,
    private val entryRepo: FormativeEntryJpaRepository,
) {
    fun listOwn(
        alunoId: UUID,
        pageable: Pageable,
    ): PageResponse<FormativaSummaryResponse> =
        PageResponse.ofWithLinks(activityRepo.findAllByIdAluno(alunoId, pageable)) { a ->
            FormativaSummaryResponse(
                id = a.id,
                titulo = a.titulo,
                categoria = a.categoria,
                cargaHoraria = a.cargaHoraria,
                estado = a.estado,
                dataRealizacao = a.dataRealizacao,
            )
        }

    fun listPendentes(pageable: Pageable): PageResponse<FormativaPendenteResponse> =
        PageResponse.ofWithLinks(activityRepo.findAllByEstado("PENDENTE", pageable)) { a ->
            FormativaPendenteResponse(
                id = a.id,
                idAluno = a.idAluno,
                titulo = a.titulo,
                categoria = a.categoria,
                cargaHoraria = a.cargaHoraria,
                dataRealizacao = a.dataRealizacao,
            )
        }

    fun resumo(alunoId: UUID): FormativaResumoResponse {
        val total = entryRepo.sumHorasAprovadas(alunoId)
        return FormativaResumoResponse(
            horasAprovadas = total,
            horasRequeridas = 120.0,
            percentual = (total / 120.0 * 100).coerceAtMost(100.0),
        )
    }
}
