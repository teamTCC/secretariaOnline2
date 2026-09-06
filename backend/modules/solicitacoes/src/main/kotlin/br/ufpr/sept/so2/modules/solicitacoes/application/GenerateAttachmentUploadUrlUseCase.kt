package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.solicitacoes.domain.AttachmentPolicy
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

data class GenerateUploadUrlCommand(
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val actorId: UUID,
    val actorAuthorities: Set<String>,
    val requestId: UUID? = null,
)

data class GenerateUploadUrlResult(
    val uploadUrl: String,
    val storageKey: String,
)

@Service
class GenerateAttachmentUploadUrlUseCase(
    private val requestRepo: RequestJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    fun execute(command: GenerateUploadUrlCommand): GenerateUploadUrlResult {
        AttachmentPolicy.assertUploadMetadata(command.contentType, command.sizeBytes)

        val storageKey =
            if (command.requestId != null) {
                val request =
                    requestRepo
                        .findById(command.requestId)
                        .orElseThrow { NoSuchElementException("Solicitação não encontrada: ${command.requestId}") }
                val isStaff = command.actorAuthorities.contains("request.deliberate")
                if (!isStaff && request.idSolicitante != command.actorId) {
                    throw AccessDeniedException("Apenas o solicitante pode gerar URL de upload.")
                }
                require(request.estado in AttachmentPolicy.MODIFIABLE_STATES) {
                    "Não é possível adicionar anexos em estado '${request.estado}'. " +
                        "Estados permitidos: ${AttachmentPolicy.MODIFIABLE_STATES.joinToString()}."
                }
                AttachmentPolicy.requestStorageKey(command.requestId, command.filename)
            } else {
                AttachmentPolicy.orphanStorageKey(command.filename)
            }

        val uploadUrl =
            minioStorageService.generateUploadUrl(
                storageKey,
                command.contentType,
                expiryMinutes = 15,
            )
        return GenerateUploadUrlResult(uploadUrl = uploadUrl, storageKey = storageKey)
    }
}
