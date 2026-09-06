package br.ufpr.sept.so2.modules.tcc.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.util.UUID

data class CreateTccDto(
    @field:NotBlank val titulo: String,
    val idCurso: UUID,
)

data class UpdateTccDto(
    val titulo: String?,
    val dataDefesa: LocalDate?,
)

data class AddMemberDto(
    val idAluno: UUID,
    val papel: String = "AUTOR",
)

data class AddExaminerDto(
    val idProfessor: UUID,
    val papel: String = "BANCA",
)

data class GradeDto(
    @field:DecimalMin("0.0") @field:DecimalMax("10.0") val nota: Double,
)

data class ApproveDto(
    val aprovado: Boolean,
    val notaFinal: Double?,
)

data class SubmitFinalUrlDto(
    @field:NotBlank val nomeOriginal: String,
)

data class SubmitFinalConfirmDto(
    @field:NotBlank val storageKey: String,
    @field:NotBlank val sha256: String,
)
