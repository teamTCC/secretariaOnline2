package br.ufpr.sept.so2.modules.iam.application

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
 * Fluxo:
 *   1. Usuário solicita via POST /me/data-export → retorna jobId (202)
 *   2. Job assíncrono agrega dados e gera arquivo JSON/CSV em MinIO
 *   3. Usuário consulta GET /me/data-export/{jobId} até status READY
 *   4. URL pré-assinada MinIO válida por 24h; arquivo deletado após expiração
 *
 * Regras:
 *   - Máximo 1 exportação por usuário por 24 horas (429 se anterior ainda pendente/válida)
 *   - Arquivo inclui: dados cadastrais, e-mails, histórico de solicitações (números/tipos),
 *     registros de presença, preferências de notificação, data de aceite LGPD
 *   - Não inclui: dados de terceiros, trilha de auditoria de outros usuários
 *
 * TODO P2: Implementar persistência do job (DataExportJobEntity + Flyway migration),
 *          agendamento assíncrono (@Async ou @Scheduled) e integração com MinIO via ArquivosAdapter.
 */
@Service
class DataExportUseCase {
    fun requestExport(command: RequestDataExportCommand): RequestDataExportResult {
        // TODO P2: verificar limite de 1 exportação/24h, persistir DataExportJob, enfileirar job assíncrono
        // Stub — auditoria será registrada pela implementação real junto ao jobId
        throw UnsupportedOperationException(
            "RF-F1-003-d (LGPD data export) — implementação pendente (P2). " +
                "Estrutura arquitetural e API definidas; aguarda sprint de privacidade.",
        )
    }

    fun getExportStatus(usuarioId: UUID, jobId: String): DataExportStatusResult {
        // TODO P2: buscar DataExportJob por jobId, verificar propriedade (usuarioId), retornar status + URL pré-assinada
        throw UnsupportedOperationException(
            "RF-F1-003-d (LGPD data export status) — implementação pendente (P2).",
        )
    }
}
