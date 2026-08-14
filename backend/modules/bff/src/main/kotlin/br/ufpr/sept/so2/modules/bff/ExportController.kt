package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ExportJobEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ExportJobJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/exports")
@Tag(name = "Secretaria — Exportações", description = "Exportação CSV assíncrona (job + MinIO)")
@PreAuthorize("hasAuthority('export.run') or hasAuthority('system.admin')")
class ExportController(
    private val exportJobRepo: ExportJobJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
    private val requestRepo: RequestJpaRepository,
    private val minio: MinioStorageService,
    private val outboxPublisher: OutboxEventPublisher,
) {
    @PostMapping("/{kind}")
    @Operation(summary = "Solicitar exportação CSV — job PROCESSANDO; worker gera o arquivo")
    @Transactional
    fun request(
        @PathVariable kind: String,
    ): ResponseEntity<Map<String, Any?>> {
        val normalized = kind.lowercase()
        require(normalized in KINDS) { "kind suportado: ${KINDS.joinToString()}" }
        val atorId = currentUserId()
        val jobId = UUID.randomUUID()
        val filename = "export-$normalized-$jobId.csv"
        val job =
            exportJobRepo.save(
                ExportJobEntity(
                    id = jobId,
                    kind = normalized,
                    status = "PROCESSANDO",
                    filename = filename,
                    storageKey = "exports/pending/$jobId",
                    idAtor = atorId,
                    expiresAt = OffsetDateTime.now().plusDays(7),
                ),
            )
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf(
                "jobId" to job.id,
                "status" to job.status,
                "expiresAt" to job.expiresAt,
                "_links" to mapOf("self" to "/exports/${job.id}", "download" to "/exports/${job.id}/download"),
            ),
        )
    }

    @GetMapping
    @Operation(summary = "Histórico de exportações do ator")
    fun list(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(exportJobRepo.findAllByIdAtor(currentUserId(), pageable)) { j -> j.toMap() }

    @GetMapping("/{jobId}")
    @Operation(summary = "Status do job de exportação")
    fun get(
        @PathVariable jobId: UUID,
    ): Map<String, Any?> {
        val job = exportJobRepo.findById(jobId).orElseThrow { NoSuchElementException("Job não encontrado: $jobId") }
        require(job.idAtor == currentUserId()) { "Acesso negado ao job $jobId" }
        return job.toMap()
    }

    @GetMapping("/{jobId}/download")
    @Operation(summary = "URL pré-assinada MinIO (ausente se EXPIRADO)")
    fun download(
        @PathVariable jobId: UUID,
    ): Map<String, Any?> {
        val job = exportJobRepo.findById(jobId).orElseThrow { NoSuchElementException("Job não encontrado: $jobId") }
        require(job.idAtor == currentUserId()) { "Acesso negado ao job $jobId" }
        if (job.status != "PRONTO" || !minio.exists(job.storageKey)) {
            return mapOf("jobId" to job.id, "status" to job.status, "downloadUrl" to null)
        }
        return mapOf(
            "jobId" to job.id,
            "status" to job.status,
            "downloadUrl" to minio.generateDownloadUrl(job.storageKey, expiryMinutes = 60),
        )
    }

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    fun processPending() {
        exportJobRepo.findAllByStatus("PROCESSANDO").forEach { job ->
            try {
                val csv = buildCsv(job.kind)
                val bytes = csv.toByteArray(Charsets.UTF_8)
                val storageKey = "exports/${job.idAtor}/${job.filename}"
                minio.upload(storageKey, bytes.inputStream(), "text/csv", bytes.size.toLong())
                job.storageKey = storageKey
                job.status = "PRONTO"
                job.errorMessage = null
                exportJobRepo.save(job)
                outboxPublisher.enqueue(
                    eventType = OutboxEventTypes.EXPORTS_READY,
                    aggregateType = "ExportJob",
                    aggregateId = job.id,
                    payload = mapOf("atorId" to job.idAtor.toString(), "kind" to job.kind),
                )
            } catch (e: Exception) {
                job.status = "ERRO"
                job.errorMessage = (e.message ?: "falha").take(2000)
                exportJobRepo.save(job)
            }
        }
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    fun expireJobs() {
        val now = OffsetDateTime.now()
        exportJobRepo.findAllByStatusAndExpiresAtBefore("PRONTO", now).forEach { job ->
            runCatching { minio.delete(job.storageKey) }
            job.status = "EXPIRADO"
            exportJobRepo.save(job)
        }
    }

    private fun buildCsv(kind: String): String =
        when (kind) {
            "alunos" -> {
                val rows = usuarioRepo.searchUsuarios(null, null, true, PageRequest.of(0, 5000)).content
                buildString {
                    appendLine("id,nome,email,grr")
                    rows.forEach { u ->
                        appendLine("${u.id},${csv(u.nome)},${csv(u.email)},${u.grr.orEmpty()}")
                    }
                }
            }
            "egressos" -> {
                val rows = usuarioRepo.findAllByRoleCode("EGRESSO", PageRequest.of(0, 5000)).content
                buildString {
                    appendLine("id,nome,email,grr")
                    rows.forEach { u ->
                        appendLine("${u.id},${csv(u.nome)},${csv(u.email)},${u.grr.orEmpty()}")
                    }
                }
            }
            else -> {
                val rows = requestRepo.findWithFilters(null, null, null, null, PageRequest.of(0, 5000)).content
                buildString {
                    appendLine("id,ano,numero,tipo,estado")
                    rows.forEach { r ->
                        appendLine("${r.id},${r.ano},${r.numeroAnual},${csv(r.requestTypeCode)},${r.estado}")
                    }
                }
            }
        }

    private fun ExportJobEntity.toMap(): Map<String, Any?> {
        val links = mutableMapOf<String, String>("self" to "/exports/$id")
        if (status == "PRONTO") links["download"] = "/exports/$id/download"
        return mapOf(
            "jobId" to id,
            "kind" to kind,
            "status" to status,
            "filename" to filename,
            "expiresAt" to expiresAt,
            "createdAt" to createdAt,
            "_links" to links,
        )
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value

    companion object {
        private val KINDS = setOf("alunos", "egressos", "solicitacoes")
    }
}
