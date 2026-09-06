package br.ufpr.sept.so2.modules.estagio.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EstagioCreatedResponse(
    val id: UUID,
    val estado: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EstagioSummaryResponse(
    val id: UUID,
    val empresa: String,
    val cargo: String,
    val estado: String,
    val inicio: LocalDate?,
    val fim: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EstagioDetailResponse(
    val id: UUID,
    val idAluno: UUID,
    val idSupervisor: UUID?,
    val empresa: String,
    val cargo: String,
    val cargaHorariaSemanal: Int,
    val estado: String,
    val inicio: LocalDate?,
    val fim: LocalDate?,
    val observacoes: String?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EstagioConcludeResponse(
    val estado: String,
    val fim: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EstagioDocumentResponse(
    val id: UUID,
    val tipo: String,
    val storageKey: String,
    val sha256: String,
    val createdAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DocumentUploadUrlResponse(
    val uploadUrl: String,
    val storageKey: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RegisteredDocumentResponse(
    val id: UUID,
    val tipo: String,
    val nomeOriginal: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoePoolItemResponse(
    val id: UUID,
    val idAluno: UUID,
    val empresa: String,
    val cargo: String,
    val cargaHorariaSemanal: Int,
    val inicio: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AssignSupervisorResponse(
    val id: UUID,
    val idSupervisor: UUID?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BulkAssignResponse(
    val processados: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoeStatsResponse(
    val semSupervisor: Long,
    val atribuidosEsteMes: Long,
)
