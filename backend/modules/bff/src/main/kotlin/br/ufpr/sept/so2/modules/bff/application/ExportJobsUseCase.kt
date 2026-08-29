package br.ufpr.sept.so2.modules.bff.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.bff.ExportDownloadResponse
import br.ufpr.sept.so2.modules.bff.ExportJobRequestedResponse
import br.ufpr.sept.so2.modules.bff.ExportJobResponse
import br.ufpr.sept.so2.modules.iam.application.ports.out.ExportJobPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.ExportJobRecord
import br.ufpr.sept.so2.modules.iam.application.ports.out.IamBffReadPort
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.modules.solicitacoes.application.ports.out.SolicitacaoBffReadPort
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Component
class ExportJobsUseCase(
    private val jobs: ExportJobPort,
    private val iam: IamBffReadPort,
    private val requests: SolicitacaoBffReadPort,
    private val minio: MinioStorageService,
    private val outboxPublisher: OutboxEventPublisher,
) {
    @Transactional
    fun request(
        kind: String,
        atorId: UUID,
    ): ExportJobRequestedResponse {
        val normalized = kind.lowercase()
        require(normalized in KINDS) { "kind suportado: ${KINDS.joinToString()}" }
        val jobId = UUID.randomUUID()
        val filename = "export-$normalized-$jobId.csv"
        val job =
            jobs.insert(
                ExportJobRecord(
                    id = jobId,
                    kind = normalized,
                    status = "PROCESSANDO",
                    filename = filename,
                    storageKey = "exports/pending/$jobId",
                    idAtor = atorId,
                    expiresAt = OffsetDateTime.now().plusDays(7),
                    createdAt = null,
                    errorMessage = null,
                ),
            )
        return ExportJobRequestedResponse(
            jobId = job.id,
            status = job.status,
            expiresAt = job.expiresAt,
            links = mapOf("self" to "/exports/${job.id}", "download" to "/exports/${job.id}/download"),
        )
    }

    fun list(
        atorId: UUID,
        page: Int,
        size: Int,
    ): PageResponse<ExportJobResponse> {
        val result = jobs.findByAtor(atorId, page, size)
        val springPage = PageImpl(result.items, PageRequest.of(page, size), result.total)
        return PageResponse.ofWithLinks(springPage) { it.toResponse() }
    }

    fun get(
        jobId: UUID,
        atorId: UUID,
    ): ExportJobResponse {
        val job = jobs.findById(jobId) ?: throw NoSuchElementException("Job não encontrado: $jobId")
        require(job.idAtor == atorId) { "Acesso negado ao job $jobId" }
        return job.toResponse()
    }

    fun download(
        jobId: UUID,
        atorId: UUID,
    ): ExportDownloadResponse {
        val job = jobs.findById(jobId) ?: throw NoSuchElementException("Job não encontrado: $jobId")
        require(job.idAtor == atorId) { "Acesso negado ao job $jobId" }
        if (job.status != "PRONTO" || !minio.exists(job.storageKey)) {
            return ExportDownloadResponse(jobId = job.id, status = job.status, downloadUrl = null)
        }
        return ExportDownloadResponse(
            jobId = job.id,
            status = job.status,
            downloadUrl = minio.generateDownloadUrl(job.storageKey, expiryMinutes = 60),
        )
    }

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    fun processPending() {
        jobs.findByStatus("PROCESSANDO").forEach { job ->
            try {
                val csv = buildCsv(job.kind)
                val bytes = csv.toByteArray(Charsets.UTF_8)
                val storageKey = "exports/${job.idAtor}/${job.filename}"
                minio.upload(storageKey, bytes.inputStream(), "text/csv", bytes.size.toLong())
                jobs.markPronto(job.id, storageKey)
                outboxPublisher.enqueue(
                    eventType = OutboxEventTypes.EXPORTS_READY,
                    aggregateType = "ExportJob",
                    aggregateId = job.id,
                    payload = mapOf("atorId" to job.idAtor.toString(), "kind" to job.kind),
                )
            } catch (e: Exception) {
                jobs.markErro(job.id, e.message ?: "falha")
            }
        }
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    fun expireJobs() {
        jobs.findReadyExpiredBefore(OffsetDateTime.now()).forEach { job ->
            runCatching { minio.delete(job.storageKey) }
            jobs.markExpirado(job.id)
        }
    }

    private fun buildCsv(kind: String): String =
        when (kind) {
            "alunos" -> {
                val rows = iam.listAlunosExport(5000)
                buildString {
                    appendLine("id,nome,email,grr")
                    rows.forEach { u ->
                        appendLine("${u.id},${csv(u.nome)},${csv(u.email)},${u.grr.orEmpty()}")
                    }
                }
            }
            "egressos" -> {
                val rows = iam.listByRoleExport("EGRESSO", 5000)
                buildString {
                    appendLine("id,nome,email,grr")
                    rows.forEach { u ->
                        appendLine("${u.id},${csv(u.nome)},${csv(u.email)},${u.grr.orEmpty()}")
                    }
                }
            }
            else -> {
                val rows = requests.listForExport(5000)
                buildString {
                    appendLine("id,ano,numero,tipo,estado")
                    rows.forEach { r ->
                        appendLine("${r.id},${r.ano},${r.numeroAnual},${csv(r.tipo)},${csv(r.estado)}")
                    }
                }
            }
        }

    private fun ExportJobRecord.toResponse(): ExportJobResponse {
        val links = mutableMapOf("self" to "/exports/$id")
        if (status == "PRONTO") links["download"] = "/exports/$id/download"
        return ExportJobResponse(
            jobId = id,
            kind = kind,
            status = status,
            filename = filename,
            expiresAt = expiresAt,
            createdAt = createdAt,
            links = links,
        )
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value

    companion object {
        val KINDS = setOf("alunos", "egressos", "solicitacoes")
    }
}
