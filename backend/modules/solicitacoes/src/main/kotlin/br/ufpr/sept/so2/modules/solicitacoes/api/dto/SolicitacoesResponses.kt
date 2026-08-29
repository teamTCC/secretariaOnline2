package br.ufpr.sept.so2.modules.solicitacoes.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequestCreatedResponse(
    val id: UUID,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DraftCreatedResponse(
    val id: UUID,
    val estado: String,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DraftSubmittedResponse(
    val id: UUID,
    val estado: String,
    val protocolo: String,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequestSummaryResponse(
    val id: UUID,
    val numeroAnual: Int,
    val ano: Int,
    val protocolo: String,
    val tipoCode: String,
    val estado: String,
    val prazoEm: OffsetDateTime?,
    val idSolicitante: UUID,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequestDetailResponse(
    val id: UUID,
    val numeroAnual: Int,
    val ano: Int,
    val protocolo: String,
    val tipoCode: String,
    val tipoDescricao: String,
    val estado: String,
    val idSolicitante: UUID,
    val dados: Any?,
    /** Included so the frontend can render dados/fields without an extra round-trip. */
    val formSchema: Any?,
    val parecer: String?,
    val prazoEm: OffsetDateTime?,
    val concludedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
    val idRequestTypeVersion: UUID? = null,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequestProtocolResponse(
    val protocolo: String,
    val tipo: String,
    val estado: String,
    val idSolicitante: UUID,
    val createdAt: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequestEventResponse(
    val tipo: String,
    val estadoAnterior: String?,
    val estadoNovo: String?,
    val parecer: String?,
    val createdAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransitionAppliedResponse(
    val mensagem: String,
    val estadoNovo: String,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BulkDeliberateResponse(
    val processados: Int,
    val action: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequestTypeSummaryResponse(
    val id: UUID,
    val code: String,
    val descricao: String,
    val prazoDias: Int?,
    val formSchema: Any?,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AttachmentUploadUrlResponse(
    val uploadUrl: String,
    val storageKey: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AttachmentResponse(
    val id: UUID,
    val categoria: String?,
    val nomeOriginal: String?,
    val contentType: String?,
    val tamanhoBytes: Long?,
    val storageKey: String,
    val sha256: String? = null,
    val createdAt: OffsetDateTime?,
)

data class AttachmentDownloadUrlResponse(
    val downloadUrl: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequestTypeDetailResponse(
    val id: UUID,
    val code: String,
    val descricao: String,
    val formSchema: Any?,
    val workflowJson: Any?,
    val prazoDias: Int,
    val ativo: Boolean,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)
