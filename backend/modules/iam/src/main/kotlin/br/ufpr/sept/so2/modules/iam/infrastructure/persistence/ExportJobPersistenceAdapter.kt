package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.modules.iam.application.ports.out.ExportJobPage
import br.ufpr.sept.so2.modules.iam.application.ports.out.ExportJobPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.ExportJobRecord
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class ExportJobPersistenceAdapter(
    private val repo: ExportJobJpaRepository,
) : ExportJobPort {
    override fun insert(job: ExportJobRecord): ExportJobRecord {
        val saved =
            repo.save(
                ExportJobEntity(
                    id = job.id,
                    kind = job.kind,
                    status = job.status,
                    filename = job.filename,
                    storageKey = job.storageKey,
                    idAtor = job.idAtor,
                    expiresAt = job.expiresAt,
                    errorMessage = job.errorMessage,
                ),
            )
        return saved.toRecord()
    }

    override fun findById(id: UUID): ExportJobRecord? =
        repo.findById(id).map { it.toRecord() }.orElse(null)

    override fun findByAtor(
        atorId: UUID,
        page: Int,
        size: Int,
    ): ExportJobPage {
        val result = repo.findAllByIdAtor(atorId, PageRequest.of(page, size))
        return ExportJobPage(
            items = result.content.map { it.toRecord() },
            total = result.totalElements,
            page = page,
            size = size,
        )
    }

    override fun findByStatus(status: String): List<ExportJobRecord> =
        repo.findAllByStatus(status).map { it.toRecord() }

    override fun findReadyExpiredBefore(now: OffsetDateTime): List<ExportJobRecord> =
        repo.findAllByStatusAndExpiresAtBefore("PRONTO", now).map { it.toRecord() }

    override fun markPronto(
        id: UUID,
        storageKey: String,
    ) {
        val job = repo.findById(id).orElseThrow { NoSuchElementException("Job não encontrado: $id") }
        job.storageKey = storageKey
        job.status = "PRONTO"
        job.errorMessage = null
        repo.save(job)
    }

    override fun markErro(
        id: UUID,
        message: String,
    ) {
        val job = repo.findById(id).orElseThrow { NoSuchElementException("Job não encontrado: $id") }
        job.status = "ERRO"
        job.errorMessage = message.take(2000)
        repo.save(job)
    }

    override fun markExpirado(id: UUID) {
        val job = repo.findById(id).orElseThrow { NoSuchElementException("Job não encontrado: $id") }
        job.status = "EXPIRADO"
        repo.save(job)
    }

    private fun ExportJobEntity.toRecord() =
        ExportJobRecord(
            id = id,
            kind = kind,
            status = status,
            filename = filename,
            storageKey = storageKey,
            idAtor = idAtor,
            expiresAt = expiresAt,
            createdAt = createdAt,
            errorMessage = errorMessage,
        )
}
