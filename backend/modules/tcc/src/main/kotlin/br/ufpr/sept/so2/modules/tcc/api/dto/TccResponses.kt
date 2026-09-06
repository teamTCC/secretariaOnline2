package br.ufpr.sept.so2.modules.tcc.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccCreatedResponse(
    val id: UUID,
    val estado: String,
    @JsonProperty("_links") val links: Map<String, String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccSummaryResponse(
    val id: UUID,
    val titulo: String,
    val estado: String,
    val dataDefesa: LocalDate?,
    val idOrientador: UUID?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccMemberResponse(
    val idAluno: UUID,
    val papel: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccExaminerResponse(
    val idProfessor: UUID,
    val papel: String,
    val nota: Double?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccDetailResponse(
    val id: UUID,
    val titulo: String,
    val idOrientador: UUID?,
    val idCurso: UUID?,
    val estado: String,
    val dataDefesa: LocalDate?,
    val notaFinal: Double?,
    val aprovado: Boolean?,
    val hashSha256Pdf: String?,
    val members: List<TccMemberResponse>,
    val examiners: List<TccExaminerResponse>,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccMemberCreatedResponse(
    val idTcc: UUID,
    val idAluno: UUID,
    val papel: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccExaminerCreatedResponse(
    val idTcc: UUID,
    val idProfessor: UUID,
    val papel: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccGradeResponse(
    val idProfessor: UUID,
    val nota: Double,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccApproveResponse(
    val estado: String,
    val aprovado: Boolean?,
    val notaFinal: Double?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccUploadUrlResponse(
    val uploadUrl: String,
    val storageKey: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TccUploadConfirmResponse(
    val id: UUID,
    val hashSha256Pdf: String?,
)
