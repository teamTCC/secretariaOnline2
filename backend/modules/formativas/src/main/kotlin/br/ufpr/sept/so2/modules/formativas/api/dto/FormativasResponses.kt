package br.ufpr.sept.so2.modules.formativas.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FormativaCreatedResponse(val id: UUID, val estado: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FormativaSummaryResponse(
    val id: UUID,
    val titulo: String,
    val categoria: String,
    val cargaHoraria: Double,
    val estado: String,
    val dataRealizacao: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FormativaPendenteResponse(
    val id: UUID,
    val idAluno: UUID,
    val titulo: String,
    val categoria: String,
    val cargaHoraria: Double,
    val dataRealizacao: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FormativaReviewedResponse(val estado: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FormativaResumoResponse(
    val horasAprovadas: Double,
    val horasRequeridas: Double,
    val percentual: Double,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ComprovanteUploadUrlResponse(
    val uploadUrl: String,
    val storageKey: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FormativaClaimedResponse(
    val id: UUID,
    val idRevisor: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BatchReviewResultResponse(
    val processadas: Int,
    val estado: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaafStatsResponse(
    val totalPendente: Long,
    val aprovadasHoje: Long,
    val rejeitadasHoje: Long,
)
