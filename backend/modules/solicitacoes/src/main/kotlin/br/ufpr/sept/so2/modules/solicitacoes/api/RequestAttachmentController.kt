package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class GenerateAttachmentUploadUrlDto(
    @field:NotBlank val filename: String,
    @field:NotBlank val contentType: String,
    @field:NotBlank val sha256: String,
    @field:Positive val sizeBytes: Long,
    @field:NotBlank val categoria: String,
)

@RestController
@RequestMapping("/requests")
@Tag(name = "Solicitações — Anexos", description = "Gestão de anexos de solicitações acadêmicas via MinIO presigned URLs")
class RequestAttachmentController(
    private val requestRepo: RequestJpaRepository,
    private val attachmentRepo: RequestAttachmentJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    @PostMapping("/attachments/presigned-url")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Gerar URL presignada para upload de anexo (orphan — vincula na submissão)")
    fun generateUploadUrl(
        @Valid @RequestBody dto: GenerateAttachmentUploadUrlDto,
    ): ResponseEntity<Map<String, String>> {
        val storageKey = "requests/orphan/${UUID.randomUUID()}_${dto.filename}"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, dto.contentType, expiryMinutes = 30)
        return ResponseEntity.ok(mapOf("uploadUrl" to uploadUrl, "storageKey" to storageKey))
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('request.view_own') or hasAuthority('request.view_curso')")
    @Operation(summary = "Listar anexos de uma solicitação")
    fun listAttachments(
        @PathVariable id: UUID,
    ): List<Map<String, Any?>> {
        assertCanAccessRequest(id)
        return attachmentRepo.findAllByIdRequest(id).map { att ->
            mapOf(
                "id" to att.id,
                "categoria" to att.categoria,
                "nomeOriginal" to att.nomeOriginal,
                "contentType" to att.contentType,
                "tamanhoBytes" to att.tamanhoBytes,
                "storageKey" to att.storageKey,
                "createdAt" to att.createdAt,
            )
        }
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download-url")
    @PreAuthorize("hasAuthority('request.view_own') or hasAuthority('request.view_curso')")
    @Operation(summary = "Gerar URL presignada para download de anexo")
    fun generateDownloadUrl(
        @PathVariable id: UUID,
        @PathVariable attachmentId: UUID,
    ): ResponseEntity<Map<String, String>> {
        assertCanAccessRequest(id)
        val attachment =
            attachmentRepo.findById(attachmentId)
                .orElseThrow { NoSuchElementException("Anexo não encontrado: $attachmentId") }
        require(attachment.idRequest == id) { "Anexo não pertence a esta solicitação." }
        val downloadUrl = minioStorageService.generateDownloadUrl(attachment.storageKey, expiryMinutes = 15)
        return ResponseEntity.ok(mapOf("downloadUrl" to downloadUrl))
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Remover anexo de uma solicitação em estado ABERTA ou RASCUNHO")
    fun deleteAttachment(
        @PathVariable id: UUID,
        @PathVariable attachmentId: UUID,
    ): ResponseEntity<Void> {
        val request = assertCanAccessRequest(id)
        require(request.idSolicitante == currentUser().userId) { "Apenas o solicitante pode remover anexos." }
        require(request.estado in listOf("ABERTA", "RASCUNHO")) {
            "Não é possível remover anexos de uma solicitação no estado '${request.estado}'."
        }
        val attachment =
            attachmentRepo.findById(attachmentId)
                .orElseThrow { NoSuchElementException("Anexo não encontrado: $attachmentId") }
        require(attachment.idRequest == id) { "Anexo não pertence a esta solicitação." }
        minioStorageService.delete(attachment.storageKey)
        attachmentRepo.delete(attachment)
        return ResponseEntity.noContent().build()
    }

    private fun assertCanAccessRequest(id: UUID): RequestEntity {
        val user = currentUser()
        val request =
            requestRepo.findById(id).orElseThrow { NoSuchElementException("Solicitação não encontrada: $id") }
        val staff =
            user.authorities.any { it == "request.view_curso" || it == "request.deliberate" }
        if (!staff && request.idSolicitante != user.userId) {
            throw AccessDeniedException("Acesso negado à solicitação $id")
        }
        return request
    }
}
