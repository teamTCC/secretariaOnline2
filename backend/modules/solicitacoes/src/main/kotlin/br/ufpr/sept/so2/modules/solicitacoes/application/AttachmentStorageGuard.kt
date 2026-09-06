package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.solicitacoes.domain.AttachmentPolicy
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Confere no MinIO que o objeto existe e que tamanho/SHA-256 batem com o informado pelo cliente.
 */
@Component
class AttachmentStorageGuard(
    private val minioStorageService: MinioStorageService,
) {
    fun assertObjectMatches(
        storageKey: String,
        expectedSha256: String,
        expectedSizeBytes: Long,
        requestId: UUID?,
    ) {
        if (requestId != null) {
            AttachmentPolicy.assertStorageKeyBindable(storageKey, requestId)
        } else {
            AttachmentPolicy.assertOrphanStorageKey(storageKey)
        }
        require(minioStorageService.exists(storageKey)) {
            "Arquivo '$storageKey' não encontrado no armazenamento. Realize o upload presignado novamente."
        }
        val actualSize = minioStorageService.objectSize(storageKey)
        require(actualSize == expectedSizeBytes) {
            "Tamanho informado ($expectedSizeBytes bytes) não confere com o arquivo armazenado ($actualSize bytes)."
        }
        val actualHash = minioStorageService.sha256(storageKey)
        require(actualHash.equals(expectedSha256, ignoreCase = true)) {
            "SHA-256 informado não confere com o arquivo armazenado."
        }
    }
}
