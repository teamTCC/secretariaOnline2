package br.ufpr.sept.so2.modules.academico.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

data class UpdateCursoDto(
    val nome: String?,
    val sigla: String?,
)

data class CreateDisciplinaDto(
    val idCurso: UUID,
    @field:NotBlank val codigo: String,
    @field:NotBlank val nome: String,
    @field:Positive val cargaHorariaTotal: Int,
    @field:Positive val creditos: Int,
)

data class CreatePeriodoDto(
    @field:Positive val ano: Short,
    @field:Positive val semestre: Short,
    val inicio: LocalDate,
    val fim: LocalDate,
)

data class UpdateCourseConfigDto(
    @field:Min(0) @field:Max(1000) val horasFormativasMinimas: Int? = null,
    val duracaoCalendario: String? = null,
    @field:Min(1) @field:Max(2) val bancaMembrosExternos: Int? = null,
    val bancaModalidade: String? = null,
    @field:Size(max = 10000) val regimento: String? = null,
)

data class UpsertHistoricoDto(
    @field:NotBlank val estado: String,
)
