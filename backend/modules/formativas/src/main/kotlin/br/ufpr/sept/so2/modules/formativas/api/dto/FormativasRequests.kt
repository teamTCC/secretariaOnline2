package br.ufpr.sept.so2.modules.formativas.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate
import java.util.UUID

data class SubmitFormativaDto(
    @field:NotBlank val titulo: String,
    val descricao: String?,
    @field:NotBlank val categoria: String,
    val cargaHoraria: Double,
    val dataRealizacao: LocalDate,
    val storageKeyComprovante: String? = null,
)

data class GenerateComprovanteUploadUrlDto(
    @field:NotBlank val filename: String,
    @field:NotBlank val contentType: String,
)

data class ReviewFormativaDto(
    @field:NotBlank val acao: String,
    val parecer: String?,
)

data class BatchReviewDto(
    @field:NotEmpty val ids: List<UUID>,
    @field:NotBlank val acao: String,
    val parecer: String? = null,
)
