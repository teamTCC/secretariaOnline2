package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.ImportJobConfirmResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ImportJobDetailResponse
import br.ufpr.sept.so2.modules.iam.application.ConfirmImportUseCase
import br.ufpr.sept.so2.modules.iam.application.ImportQuery
import br.ufpr.sept.so2.modules.iam.application.UploadImportUseCase
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/imports")
@Tag(name = "Secretaria — Importações", description = "Importação CSV de alunos em duas fases (validar + confirmar)")
@PreAuthorize("hasAuthority('import.run') or hasAuthority('system.admin')")
class ImportController(
    private val importQuery: ImportQuery,
    private val uploadImportUseCase: UploadImportUseCase,
    private val confirmImportUseCase: ConfirmImportUseCase,
) {
    @GetMapping("/templates/{kind}")
    @Operation(summary = "Baixar modelo CSV")
    fun template(
        @PathVariable kind: String,
    ): ResponseEntity<String> {
        val result = importQuery.template(kind)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.filename}\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(result.csv)
    }

    @PostMapping("/{kind}")
    @Operation(summary = "Upload CSV — valida e grava job VALIDATED (ainda não persiste usuários)")
    fun upload(
        @PathVariable kind: String,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<ImportJobDetailResponse> {
        val body =
            uploadImportUseCase.execute(
                kind = kind,
                filename = file.originalFilename ?: "upload.csv",
                bytes = file.bytes,
                empty = file.isEmpty,
                size = file.size,
                actorId = currentUserId(),
            )
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body)
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Status do job de importação")
    fun get(
        @PathVariable jobId: UUID,
    ): ImportJobDetailResponse = importQuery.get(jobId)

    @PostMapping("/{jobId}/confirm")
    @Operation(summary = "Confirmar importação válida — cria usuários")
    fun confirm(
        @PathVariable jobId: UUID,
    ): ResponseEntity<ImportJobConfirmResponse> {
        val result = confirmImportUseCase.execute(jobId)
        return ResponseEntity.ok(
            ImportJobConfirmResponse(
                jobId = result.jobId,
                status = result.status,
                successCount = result.successCount,
                errorCount = result.errorCount,
            ),
        )
    }
}
