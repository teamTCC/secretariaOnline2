package br.ufpr.sept.so2.modules.presenca.domain

import java.time.OffsetDateTime
import java.util.UUID

enum class AttendanceMode {
    QR_SINGLE,
    QR_DUAL,
    SECRET_SINGLE,
    SECRET_DUAL,
    ;

    fun isDual(): Boolean = this == QR_DUAL || this == SECRET_DUAL

    fun isQr(): Boolean = this == QR_SINGLE || this == QR_DUAL

    fun isSecret(): Boolean = this == SECRET_SINGLE || this == SECRET_DUAL
}

enum class EventState {
    AGENDADO,
    EM_ANDAMENTO,
    CONCLUIDO,
    CANCELADO,
    ;

    fun canOpenWindow(): Boolean = this == EM_ANDAMENTO
}

enum class AttendancePhase { ENTRY, EXIT }

data class ValidationWindow(
    val phase: AttendancePhase,
    val openAt: OffsetDateTime,
    val closeAt: OffsetDateTime,
    val secret: String?,
    val qrToken: String?,
) {
    fun isActive(): Boolean {
        val now = OffsetDateTime.now()
        return now.isAfter(openAt) && now.isBefore(closeAt)
    }
}

data class EventAttendance(
    val id: UUID,
    val titulo: String,
    val descricao: String?,
    val idOrganizador: UUID,
    val idCurso: UUID?,
    val attendanceMode: AttendanceMode,
    val estado: EventState,
    val chCreditadas: Double,
    val inicioEm: OffsetDateTime,
    val fimEm: OffsetDateTime,
    val validationWindows: List<ValidationWindow>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun activeWindowForPhase(phase: AttendancePhase): ValidationWindow? =
        validationWindows.firstOrNull { it.phase == phase && it.isActive() }

    fun hasActiveEntryWindow(): Boolean = activeWindowForPhase(AttendancePhase.ENTRY) != null

    fun hasActiveExitWindow(): Boolean = attendanceMode.isDual() && activeWindowForPhase(AttendancePhase.EXIT) != null

    fun isEligibleForCheckin(): Boolean = estado == EventState.EM_ANDAMENTO && hasActiveEntryWindow()
}

data class AttendanceSession(
    val id: UUID,
    val idEvento: UUID,
    val idAluno: UUID,
    val deviceUuid: String?,
    val entryConfirmedAt: OffsetDateTime?,
    val exitConfirmedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
) {
    fun isComplete(mode: AttendanceMode): Boolean =
        when {
            mode.isDual() -> entryConfirmedAt != null && exitConfirmedAt != null
            else -> entryConfirmedAt != null
        }

    fun hasEntryConfirmed(): Boolean = entryConfirmedAt != null
}
