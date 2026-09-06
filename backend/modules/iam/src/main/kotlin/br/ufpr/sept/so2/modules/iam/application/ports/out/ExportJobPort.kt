package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.time.OffsetDateTime
import java.util.UUID

data class ExportJobRecord(
    val id: UUID,
    val kind: String,
    val status: String,
    val filename: String,
    val storageKey: String,
    val idAtor: UUID,
    val expiresAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
    val errorMessage: String?,
)

data class ExportJobPage(
    val items: List<ExportJobRecord>,
    val total: Long,
    val page: Int,
    val size: Int,
)

interface ExportJobPort {
    fun insert(job: ExportJobRecord): ExportJobRecord

    fun findById(id: UUID): ExportJobRecord?

    fun findByAtor(
        atorId: UUID,
        page: Int,
        size: Int,
    ): ExportJobPage

    fun findByStatus(status: String): List<ExportJobRecord>

    fun findReadyExpiredBefore(now: OffsetDateTime): List<ExportJobRecord>

    fun markPronto(
        id: UUID,
        storageKey: String,
    )

    fun markErro(
        id: UUID,
        message: String,
    )

    fun markExpirado(id: UUID)
}
