package br.ufpr.sept.so2.modules.estagio.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate
import java.util.UUID

data class DeclararEstagioDto(
    @field:NotBlank val empresa: String,
    @field:NotBlank val cargo: String,
    @field:Min(1) val cargaHorariaSemanal: Int,
    val inicio: LocalDate,
    val observacoes: String?,
)

data class AtualizarEstagioDto(
    val cargo: String?,
    val cargaHorariaSemanal: Int?,
    val fim: LocalDate?,
    val observacoes: String?,
    val idSupervisor: UUID?,
)

data class GenerateUploadUrlDto(
    @field:NotBlank val tipo: String,
    @field:NotBlank val nomeOriginal: String,
    @field:NotBlank val contentType: String,
)

data class RegisterDocumentDto(
    @field:NotBlank val tipo: String,
    @field:NotBlank val storageKey: String,
    @field:NotBlank val sha256: String,
    @field:NotBlank val nomeOriginal: String,
)

data class AssignSupervisorDto(
    val idSupervisor: UUID,
)

data class BulkAssignSupervisorDto(
    @field:NotEmpty val internshipIds: List<UUID>,
    val idSupervisor: UUID,
)
