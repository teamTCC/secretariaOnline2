package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.api.dto.AttachmentDownloadUrlResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.AttachmentInputDto
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.AttachmentResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.AttachmentUploadUrlResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.GenerateAttachmentUploadUrlDto
import br.ufpr.sept.so2.modules.solicitacoes.application.AttachmentInput
import br.ufpr.sept.so2.modules.solicitacoes.application.ConfirmAttachmentCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.ConfirmAttachmentUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.DeleteAttachmentCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.DeleteAttachmentUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.GenerateAttachmentUploadUrlUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.GenerateUploadUrlCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.RequestAttachmentQuery
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/requests")
@Tag(name = "Solicitações — Anexos", description = "Gestão de anexos de solicitações acadêmicas via MinIO presigned URLs")
class RequestAttachmentController(
    private val requestAttachmentQuery: RequestAttachmentQuery,
    private val generateUploadUrlUseCase: GenerateAttachmentUploadUrlUseCase,
    private val confirmAttachmentUseCase: ConfirmAttachmentUseCase,
    private val deleteAttachmentUseCase: DeleteAttachmentUseCase,
) {
    /**
     * Wizard (F1.8): gera URL órfã antes de existir o request.
     * Após POST /draft ou POST /requests, preferir POST /{id}/attachments/upload-url.
     */
    @PostMapping("/attachments/presigned-url")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Gerar URL presignada órfã (wizard — vincula na submissão ou no confirm)")
    fun generateOrphanUploadUrl(
        @Valid @RequestBody dto: GenerateAttachmentUploadUrlDto,
    ): ResponseEntity<AttachmentUploadUrlResponse> {
        val user = currentUser()
        val result =
            generateUploadUrlUseCase.execute(
                GenerateUploadUrlCommand(
                    filename = dto.filename,
                    contentType = dto.contentType,
                    sizeBytes = dto.sizeBytes,
                    actorId = user.userId,
                    actorAuthorities = user.authorities,
                    requestId = null,
                ),
            )
        return ResponseEntity.ok(
            AttachmentUploadUrlResponse(uploadUrl = result.uploadUrl, storageKey = result.storageKey),
        )
    }

    /** Path canônico HU / MVP v2: upload vinculado a uma solicitação já persistida (rascunho ou aberta). */
    @PostMapping("/{id}/attachments/upload-url")
    @PreAuthorize("hasAuthority('request.open') or hasAuthority('request.deliberate')")
    @Operation(summary = "Gerar URL presignada vinculada à solicitação")
    fun generateBoundUploadUrl(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: GenerateAttachmentUploadUrlDto,
    ): ResponseEntity<AttachmentUploadUrlResponse> {
        val user = currentUser()
        val result =
            generateUploadUrlUseCase.execute(
                GenerateUploadUrlCommand(
                    filename = dto.filename,
                    contentType = dto.contentType,
                    sizeBytes = dto.sizeBytes,
                    actorId = user.userId,
                    actorAuthorities = user.authorities,
                    requestId = id,
                ),
            )
        return ResponseEntity.ok(
            AttachmentUploadUrlResponse(uploadUrl = result.uploadUrl, storageKey = result.storageKey),
        )
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('request.view_own') or hasAuthority('request.view_curso')")
    @Operation(summary = "Listar anexos de uma solicitação")
    fun listAttachments(
        @PathVariable id: UUID,
    ): List<AttachmentResponse> {
        val user = currentUser()
        return requestAttachmentQuery.listAttachments(id, user.userId, user.authorities)
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download-url")
    @PreAuthorize("hasAuthority('request.view_own') or hasAuthority('request.view_curso')")
    @Operation(summary = "Gerar URL presignada para download de anexo")
    fun generateDownloadUrl(
        @PathVariable id: UUID,
        @PathVariable attachmentId: UUID,
    ): ResponseEntity<AttachmentDownloadUrlResponse> {
        val user = currentUser()
        return ResponseEntity.ok(
            requestAttachmentQuery.generateDownloadUrl(id, attachmentId, user.userId, user.authorities),
        )
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Remover anexo de uma solicitação em RASCUNHO, ABERTA ou EM_AJUSTE")
    fun deleteAttachment(
        @PathVariable id: UUID,
        @PathVariable attachmentId: UUID,
    ): ResponseEntity<Void> {
        val user = currentUser()
        deleteAttachmentUseCase.execute(
            DeleteAttachmentCommand(
                requestId = id,
                attachmentId = attachmentId,
                actorId = user.userId,
                actorAuthorities = user.authorities,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/attachments/confirm")
    @PreAuthorize("hasAuthority('request.open') or hasAuthority('request.deliberate')")
    @Operation(summary = "Confirmar upload e vincular anexo à solicitação (valida MinIO + SHA-256)")
    fun confirmAttachment(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AttachmentInputDto,
    ): ResponseEntity<AttachmentResponse> {
        val user = currentUser()
        val attachmentId =
            confirmAttachmentUseCase.execute(
                ConfirmAttachmentCommand(
                    requestId = id,
                    actorId = user.userId,
                    actorAuthorities = user.authorities,
                    attachment = dto.toInput(),
                ),
            )
        return ResponseEntity.status(201).body(requestAttachmentQuery.getById(attachmentId))
    }

    private fun AttachmentInputDto.toInput() =
        AttachmentInput(
            storageKey = storageKey,
            sha256 = sha256,
            nomeOriginal = nomeOriginal,
            contentType = contentType,
            categoria = categoria,
            tamanhoBytes = tamanhoBytes,
        )
}
