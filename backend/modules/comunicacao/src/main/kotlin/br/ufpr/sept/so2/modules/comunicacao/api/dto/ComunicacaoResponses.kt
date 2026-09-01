package br.ufpr.sept.so2.modules.comunicacao.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommunicationPublishedResponse(val id: UUID, val entregas: Int)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommunicationSummaryResponse(
    val id: UUID,
    val titulo: String,
    val tipo: String,
    val publishedAt: OffsetDateTime?,
    val audiencia: Map<String, Any>?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommunicationDeliveryResponse(
    val deliveryId: UUID,
    val idCommunication: UUID,
    val canal: String,
    val status: String,
    val deliveredAt: OffsetDateTime?,
    val readAt: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UnreadCountResponse(val unread: Long)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommunicationDetailResponse(
    val id: UUID,
    val titulo: String,
    val conteudo: String,
    val tipo: String,
    val audiencia: Map<String, Any>?,
    val publishedAt: OffsetDateTime?,
    val expiresAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TemplateCreatedResponse(val id: UUID, val codigo: String, val versao: Int)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TemplateRevisedResponse(val id: UUID, val versao: Int)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TemplateSummaryResponse(
    val id: UUID,
    val codigo: String,
    val titulo: String,
    val assunto: String,
    val corpo: String,
    val canal: String,
    val versao: Int,
    val ativo: Boolean,
    val variaveis: List<String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TemplateRevisionSummaryResponse(
    val versao: Int,
    val assunto: String,
    val createdAt: OffsetDateTime,
    val idAutor: UUID?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TemplateRevisionDetailResponse(
    val versao: Int,
    val assunto: String,
    val corpo: String,
    val createdAt: OffsetDateTime,
)
