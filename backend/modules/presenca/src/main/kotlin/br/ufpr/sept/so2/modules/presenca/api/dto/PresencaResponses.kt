package br.ufpr.sept.so2.modules.presenca.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventCreatedResponse(
    val id: UUID,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventSummaryResponse(
    val id: UUID,
    val titulo: String,
    val attendanceMode: String,
    val estado: String,
    val chCreditadas: Double,
    val inicioEm: OffsetDateTime?,
    val fimEm: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventDetailResponse(
    val id: UUID,
    val titulo: String,
    val descricao: String?,
    val attendanceMode: String,
    val estado: String,
    val chCreditadas: Double,
    val inicioEm: OffsetDateTime?,
    val fimEm: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AttendanceSessionResponse(
    val idEvento: UUID,
    val estado: String,
    val attendanceMode: String,
    val entryConfirmedAt: OffsetDateTime?,
    val exitConfirmedAt: OffsetDateTime?,
    val isComplete: Boolean,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AttendanceConfirmedResponse(
    val mensagem: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WindowOpenedResponse(
    val mensagem: String,
    val closeAt: String,
    val secret: String? = null,
    val qrToken: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventClosedResponse(
    val mensagem: String,
    val certificadosEmitidos: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CertificateSummaryResponse(
    val id: UUID,
    val idEvento: UUID?,
    val origem: String,
    val hashSha256: String,
    val chCreditadas: Double,
    val issuedAt: OffsetDateTime,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DownloadUrlResponse(
    val downloadUrl: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CertificateVerificationResponse(
    val valido: Boolean,
    val hashSha256: String,
    val chCreditadas: Double,
    val issuedAt: OffsetDateTime,
    val idEvento: UUID?,
    val origem: String,
    val integridadePdf: Boolean,
    val verificacaoAssinatura: String,
    val ephemeralKey: Boolean,
    @JsonProperty("_links") val links: Map<String, String>,
)
