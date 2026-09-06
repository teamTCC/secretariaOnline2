package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.estagio.api.dto.EstagioDocumentResponse
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipDocumentJpaRepository
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EstagioDocumentQuery(
    private val internshipRepo: InternshipJpaRepository,
    private val documentRepo: InternshipDocumentJpaRepository,
) {
    fun list(
        internshipId: UUID,
        userId: UUID,
        authorities: Set<String>,
    ): List<EstagioDocumentResponse> {
        val internship =
            internshipRepo.findById(internshipId)
                .orElseThrow { NoSuchElementException("Estágio não encontrado: $internshipId") }
        val isOwner = internship.idAluno == userId
        val isSupervisor = internship.idSupervisor == userId
        val canReview = authorities.contains("internship.review")
        if (!isOwner && !isSupervisor && !canReview) {
            throw AccessDeniedException("Acesso negado aos documentos do estágio ${internship.id}")
        }
        return documentRepo.findAllByIdInternship(internshipId).map { doc ->
            EstagioDocumentResponse(
                id = doc.id,
                tipo = doc.tipo,
                storageKey = doc.storageKey,
                sha256 = doc.sha256,
                createdAt = doc.uploadedAt,
            )
        }
    }
}
