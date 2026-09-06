package br.ufpr.sept.so2.modules.solicitacoes.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import java.util.UUID

data class AttachmentInputDto(
    @field:NotBlank val storageKey: String,
    @field:NotBlank
    @field:Pattern(
        regexp = "^[a-fA-F0-9]{64}$",
        message = "sha256 deve ser um hex de exatamente 64 caracteres",
    )
    val sha256: String,
    @field:NotBlank val nomeOriginal: String,
    @field:NotBlank val contentType: String,
    @field:NotBlank val categoria: String,
    @field:Positive val tamanhoBytes: Long,
)

data class OpenRequestDto(
    val idRequestType: UUID,
    val idCurso: UUID,
    val dados: Map<String, Any>,
    val attachments: List<AttachmentInputDto> = emptyList(),
    /** Secretaria only (requires request.open_on_behalf authority). Opens the request on behalf of this student. */
    val idSolicitanteOnBehalf: UUID? = null,
)

data class TransitionDto(
    @field:NotBlank val action: String,
    val parecer: String?,
)

data class BulkDeliberateDto(
    @field:NotEmpty val ids: List<UUID>,
    @field:NotBlank val action: String,
    val parecer: String? = null,
)

data class UpdateDraftDto(
    val dados: Map<String, Any>,
)

data class GenerateAttachmentUploadUrlDto(
    @field:NotBlank val filename: String,
    @field:NotBlank val contentType: String,
    @field:NotBlank val sha256: String,
    @field:Positive val sizeBytes: Long,
    @field:NotBlank val categoria: String,
)

data class UpsertRequestTypeDto(
    @field:NotBlank val code: String,
    @field:NotBlank val descricao: String,
    val formSchema: Map<String, Any> = emptyMap(),
    val workflowJson: Map<String, Any> = emptyMap(),
    val prazoDias: Int = 10,
)
