package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioDetailResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioSummaryResponse
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipEntity
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EstagioQuery(
    private val internshipRepo: InternshipJpaRepository,
) {
    fun mine(
        userId: UUID,
        pageable: Pageable,
    ): PageResponse<EstagioSummaryResponse> =
        PageResponse.ofWithLinks(internshipRepo.findAllByIdAluno(userId, pageable)) { toSummary(it) }

    fun list(
        estado: String,
        userId: UUID,
        authorities: Set<String>,
        pageable: Pageable,
    ): PageResponse<EstagioSummaryResponse> {
        val page =
            if (authorities.contains("internship.supervise") && !authorities.contains("internship.review")) {
                internshipRepo.findAllByIdSupervisor(userId, pageable)
            } else {
                internshipRepo.findAllByEstado(estado, pageable)
            }
        return PageResponse.ofWithLinks(page) { toSummary(it) }
    }

    fun get(
        id: UUID,
        userId: UUID,
        authorities: Set<String>,
    ): EstagioDetailResponse {
        val internship =
            internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        val isOwner = internship.idAluno == userId
        val isSupervisor = internship.idSupervisor == userId
        val canReview = authorities.contains("internship.review")
        if (!isOwner && !isSupervisor && !canReview) {
            throw AccessDeniedException("Acesso negado ao estágio $id")
        }
        val links = linkedMapOf("self" to "/internships/$id", "documents" to "/internships/$id/documents")
        if (canReview || isSupervisor) links["update"] = "/internships/$id"
        if (canReview) links["conclude"] = "/internships/$id/conclude"
        return toDetail(internship, links)
    }

    fun toDetail(
        internship: InternshipEntity,
        links: Map<String, String> = emptyMap(),
    ): EstagioDetailResponse =
        EstagioDetailResponse(
            id = internship.id,
            idAluno = internship.idAluno,
            idSupervisor = internship.idSupervisor,
            empresa = internship.empresa,
            cargo = internship.cargo,
            cargaHorariaSemanal = internship.cargaHorariaSemanal,
            estado = internship.estado,
            inicio = internship.inicio,
            fim = internship.fim,
            observacoes = internship.observacoes,
            createdAt = internship.createdAt,
            updatedAt = internship.updatedAt,
            links = links,
        )

    private fun toSummary(entity: InternshipEntity): EstagioSummaryResponse =
        EstagioSummaryResponse(
            id = entity.id,
            empresa = entity.empresa,
            cargo = entity.cargo,
            estado = entity.estado,
            inicio = entity.inicio,
            fim = entity.fim,
        )
}
