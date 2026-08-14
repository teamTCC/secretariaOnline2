package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

data class RequestDataExportCommand(
    val usuarioId: UUID,
    val ip: String?,
    val userAgent: String? = null,
)

data class RequestDataExportResult(
    val jobId: UUID,
    val downloadUrl: String? = null,
)

enum class DataExportStatus { PENDING, READY, EXPIRED }

data class DataExportStatusResult(
    val jobId: UUID,
    val status: DataExportStatus,
    val downloadUrl: String?,
    val expiresAt: OffsetDateTime?,
)

/**
 * RF-F1-003-d — Exportação de dados pessoais (LGPD Art. 18, III).
 *
 * Implementação síncrona: gera o JSON, envia ao MinIO e retorna URL pré-assinada (24h)
 * imediatamente na resposta 202. Sem persistência de job — o downloadUrl é a prova de entrega.
 */
@Service
class DataExportUseCase(
    private val usuarioRepo: UsuarioJpaRepository,
    private val minioStorageService: MinioStorageService,
    private val objectMapper: ObjectMapper,
) {
    fun requestExport(command: RequestDataExportCommand): RequestDataExportResult {
        val usuario = usuarioRepo.findById(command.usuarioId)
            .orElseThrow { NoSuchElementException("Usuário não encontrado: ${command.usuarioId}") }

        val exportData = mapOf(
            "exportadoEm" to OffsetDateTime.now().toString(),
            "id" to usuario.id,
            "nome" to usuario.nome,
            "email" to usuario.email,
            "grr" to usuario.grr,
            "idCurso" to usuario.metadata["idCurso"],
            "ativo" to usuario.ativo,
            "createdAt" to usuario.createdAt,
            "metadata" to usuario.metadata,
            "aviso" to "Exportação LGPD Art. 18, III — SecretariaOnline2 UFPR",
        )

        val jsonBytes = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsBytes(exportData)

        val jobId = UUID.randomUUID()
        val storageKey = "exports/${command.usuarioId}/data_export_${jobId}.json"

        minioStorageService.upload(
            storageKey = storageKey,
            inputStream = jsonBytes.inputStream(),
            contentType = "application/json",
            size = jsonBytes.size.toLong(),
        )

        val downloadUrl = minioStorageService.generateDownloadUrl(storageKey, expiryMinutes = 1440)

        return RequestDataExportResult(jobId = jobId, downloadUrl = downloadUrl)
    }

    fun getExportStatus(usuarioId: UUID, jobId: String): DataExportStatusResult {
        val parsedJobId = UUID.fromString(jobId)
        val storageKey = "exports/$usuarioId/data_export_$parsedJobId.json"
        if (!minioStorageService.exists(storageKey)) {
            return DataExportStatusResult(
                jobId = parsedJobId,
                status = DataExportStatus.EXPIRED,
                downloadUrl = null,
                expiresAt = null,
            )
        }
        val downloadUrl = minioStorageService.generateDownloadUrl(storageKey, expiryMinutes = 1440)
        return DataExportStatusResult(
            jobId = parsedJobId,
            status = DataExportStatus.READY,
            downloadUrl = downloadUrl,
            expiresAt = OffsetDateTime.now().plusMinutes(1440),
        )
    }
}
