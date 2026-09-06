package br.ufpr.sept.so2.modules.bff

import br.ufpr.sept.so2.modules.bff.application.ExportJobsUseCase
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUserId
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExportJobResponse(
    val jobId: UUID,
    val kind: String,
    val status: String,
    val filename: String,
    val expiresAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExportJobRequestedResponse(
    val jobId: UUID,
    val status: String,
    val expiresAt: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExportDownloadResponse(
    val jobId: UUID,
    val status: String,
    val downloadUrl: String?,
)

@RestController
@RequestMapping("/exports")
@Tag(name = "Secretaria — Exportações", description = "Exportação CSV assíncrona (job + MinIO)")
@PreAuthorize("hasAuthority('export.run') or hasAuthority('system.admin')")
class ExportController(
    private val exportJobs: ExportJobsUseCase,
) {
    @PostMapping("/{kind}")
    @Operation(summary = "Solicitar exportação CSV — job PROCESSANDO; worker gera o arquivo")
    fun request(
        @PathVariable kind: String,
    ): ResponseEntity<ExportJobRequestedResponse> =
        ResponseEntity.status(HttpStatus.ACCEPTED).body(exportJobs.request(kind, currentUserId()))

    @GetMapping
    @Operation(summary = "Histórico de exportações do ator")
    fun list(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<ExportJobResponse> =
        exportJobs.list(currentUserId(), pageable.pageNumber, pageable.pageSize)

    @GetMapping("/{jobId}")
    @Operation(summary = "Status do job de exportação")
    fun get(
        @PathVariable jobId: UUID,
    ): ExportJobResponse = exportJobs.get(jobId, currentUserId())

    @GetMapping("/{jobId}/download")
    @Operation(summary = "URL pré-assinada MinIO (ausente se EXPIRADO)")
    fun download(
        @PathVariable jobId: UUID,
    ): ExportDownloadResponse = exportJobs.download(jobId, currentUserId())
}
