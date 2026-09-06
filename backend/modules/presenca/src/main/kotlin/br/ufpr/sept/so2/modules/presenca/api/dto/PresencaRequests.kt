package br.ufpr.sept.so2.modules.presenca.api.dto

import br.ufpr.sept.so2.modules.presenca.domain.AttendanceMode
import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime
import java.util.UUID

data class CreateEventDto(
    @field:NotBlank val titulo: String,
    val descricao: String?,
    val idCurso: UUID?,
    val attendanceMode: AttendanceMode,
    val chCreditadas: Double,
    val inicioEm: OffsetDateTime,
    val fimEm: OffsetDateTime,
)

data class ConfirmAttendanceDto(
    val pin: String?,
    val qrToken: String?,
    val deviceUuid: String?,
)

data class OpenWindowDto(
    val durationSeconds: Int?,
)
