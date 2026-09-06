package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.AttachmentDownloadUrlResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.AttachmentResponse
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RequestAttachmentQuery(
    private val requestRepo: RequestJpaRepository,
    private val attachmentRepo: RequestAttachmentJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    fun listAttachments(
        requestId: UUID,
        actorId: UUID,
        actorAuthorities: Set<String>,
    ): List<AttachmentResponse> {
        assertCanAccessRequest(requestId, actorId, actorAuthorities)
        return attachmentRepo.findAllByIdRequest(requestId).map { toResponse(it) }
    }

    fun generateDownloadUrl(
        requestId: UUID,
        attachmentId: UUID,
        actorId: UUID,
        actorAuthorities: Set<String>,
    ): AttachmentDownloadUrlResponse {
        assertCanAccessRequest(requestId, actorId, actorAuthorities)
        val attachment =
            attachmentRepo.findById(attachmentId)
                .orElseThrow { NoSuchElementException("Anexo não encontrado: $attachmentId") }
        require(attachment.idRequest == requestId) { "Anexo não pertence a esta solicitação." }
        val downloadUrl = minioStorageService.generateDownloadUrl(attachment.storageKey, expiryMinutes = 15)
        return AttachmentDownloadUrlResponse(downloadUrl = downloadUrl)
    }

    fun getById(attachmentId: UUID): AttachmentResponse {
        val entity =
            attachmentRepo.findById(attachmentId)
                .orElseThrow { NoSuchElementException("Anexo não encontrado: $attachmentId") }
        return toResponse(entity)
    }

    fun toResponse(entity: RequestAttachmentEntity) =
        AttachmentResponse(
            id = entity.id,
            categoria = entity.categoria,
            nomeOriginal = entity.nomeOriginal,
            contentType = entity.contentType,
            tamanhoBytes = entity.tamanhoBytes,
            storageKey = entity.storageKey,
            sha256 = entity.sha256,
            createdAt = entity.createdAt,
        )

    private fun assertCanAccessRequest(
        id: UUID,
        actorId: UUID,
        actorAuthorities: Set<String>,
    ): RequestEntity {
        val request =
            requestRepo.findById(id).orElseThrow { NoSuchElementException("Solicitação não encontrada: $id") }
        val staff =
            actorAuthorities.any { it == "request.view_curso" || it == "request.deliberate" }
        if (!staff && request.idSolicitante != actorId) {
            throw AccessDeniedException("Acesso negado à solicitação $id")
        }
        return request
    }
}
