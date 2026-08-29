package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.solicitacoes.domain.AttachmentPolicy
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class DeleteAttachmentCommand(
    val requestId: UUID,
    val attachmentId: UUID,
    val actorId: UUID,
    val actorAuthorities: Set<String>,
)

@Service
class DeleteAttachmentUseCase(
    private val requestRepo: RequestJpaRepository,
    private val attachmentRepo: RequestAttachmentJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    @Transactional
    fun execute(command: DeleteAttachmentCommand) {
        val request =
            requestRepo.findById(command.requestId)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: ${command.requestId}") }
        val staff =
            command.actorAuthorities.any { it == "request.view_curso" || it == "request.deliberate" }
        if (!staff && request.idSolicitante != command.actorId) {
            throw AccessDeniedException("Acesso negado à solicitação ${command.requestId}")
        }
        require(request.idSolicitante == command.actorId) { "Apenas o solicitante pode remover anexos." }
        require(request.estado in AttachmentPolicy.MODIFIABLE_STATES) {
            "Não é possível remover anexos de uma solicitação no estado '${request.estado}'."
        }
        val attachment =
            attachmentRepo.findById(command.attachmentId)
                .orElseThrow { NoSuchElementException("Anexo não encontrado: ${command.attachmentId}") }
        require(attachment.idRequest == command.requestId) { "Anexo não pertence a esta solicitação." }
        minioStorageService.delete(attachment.storageKey)
        attachmentRepo.delete(attachment)
    }
}
