package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.domain.AttachmentPolicy
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ConfirmAttachmentCommand(
    val requestId: UUID,
    val actorId: UUID,
    val actorAuthorities: Set<String>,
    val attachment: AttachmentInput,
)

@Service
class ConfirmAttachmentUseCase(
    private val requestRepo: RequestJpaRepository,
    private val attachmentRepo: RequestAttachmentJpaRepository,
    private val storageGuard: AttachmentStorageGuard,
    private val auditPublisher: AuditPublisher,
) {
    @Transactional
    fun execute(command: ConfirmAttachmentCommand): UUID {
        val request =
            requestRepo
                .findById(command.requestId)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: ${command.requestId}") }

        val isStaff = command.actorAuthorities.contains("request.deliberate")
        if (!isStaff && request.idSolicitante != command.actorId) {
            throw AccessDeniedException("Apenas o solicitante pode adicionar anexos.")
        }

        require(request.estado in AttachmentPolicy.MODIFIABLE_STATES) {
            "Não é possível adicionar anexos em estado '${request.estado}'. " +
                "Estados permitidos: ${AttachmentPolicy.MODIFIABLE_STATES.joinToString()}."
        }

        AttachmentPolicy.assertUploadMetadata(command.attachment.contentType, command.attachment.tamanhoBytes)
        storageGuard.assertObjectMatches(
            storageKey = command.attachment.storageKey,
            expectedSha256 = command.attachment.sha256,
            expectedSizeBytes = command.attachment.tamanhoBytes,
            requestId = command.requestId,
        )

        val saved =
            attachmentRepo.save(
                RequestAttachmentEntity(
                    idRequest = command.requestId,
                    categoria = command.attachment.categoria,
                    storageKey = command.attachment.storageKey,
                    sha256 = command.attachment.sha256.lowercase(),
                    nomeOriginal = command.attachment.nomeOriginal,
                    contentType = command.attachment.contentType,
                    tamanhoBytes = command.attachment.tamanhoBytes,
                ),
            )

        auditPublisher.publish(
            AuditPayload(
                acao = "attachment_uploaded",
                idAtor = command.actorId,
                alvoTipo = "Request",
                alvoId = command.requestId,
                ip = null,
                userAgent = null,
                resultado = "OK",
                detalhes =
                    mapOf(
                        "attachmentId" to saved.id.toString(),
                        "categoria" to saved.categoria,
                        "sha256" to saved.sha256,
                    ),
            ),
        )

        return saved.id
    }
}
