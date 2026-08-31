package br.ufpr.sept.so2.modules.presenca.application

import br.ufpr.sept.so2.modules.presenca.domain.AttendanceMode
import br.ufpr.sept.so2.modules.presenca.domain.AttendancePhase
import br.ufpr.sept.so2.modules.presenca.domain.EventNotFoundException
import br.ufpr.sept.so2.modules.presenca.domain.EventState
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class OpenWindowCommand(
    val eventId: UUID,
    val phase: AttendancePhase,
    val durationSeconds: Int,
    val requestingUserId: UUID,
    val requestingUserAuthorities: Collection<String>,
)

data class CloseEventCommand(
    val eventId: UUID,
    val requestingUserId: UUID,
    val requestingUserAuthorities: Collection<String>,
)

data class OpenWindowResult(
    val message: String,
    val closeAt: String,
    val secret: String? = null,
    val qrToken: String? = null,
)

@Service
@Transactional
class ManageEventUseCase(
    private val eventRepo: EventAttendanceJpaRepository,
    private val certificateIssuerService: CertificateIssuerService,
) {
    fun openWindow(command: OpenWindowCommand): OpenWindowResult {
        val event = eventRepo.findById(command.eventId)
            .orElseThrow { EventNotFoundException(command.eventId) }

        val isOrganizador = event.idOrganizador == command.requestingUserId
        val canManage = command.requestingUserAuthorities.contains("event.manage")
        if (!isOrganizador && !canManage) {
            throw AccessDeniedException("Apenas o organizador pode abrir janelas")
        }

        val mode = AttendanceMode.valueOf(event.attendanceMode)

        require(event.estado != EventState.CONCLUIDO.name) {
            "Evento já encerrado."
        }

        if (command.phase == AttendancePhase.EXIT) {
            require(mode.isDual()) { "Este evento não usa modo dual de presença." }
            require(event.estado == EventState.EM_ANDAMENTO.name) {
                "Evento precisa estar em andamento para abrir a janela de saída."
            }
        }

        // Mutate the managed entity — a bulk UPDATE followed by save() would overwrite AGENDADO.
        if (command.phase == AttendancePhase.ENTRY && event.estado == EventState.AGENDADO.name) {
            event.estado = EventState.EM_ANDAMENTO.name
        }

        val now = OffsetDateTime.now()
        val window: Map<String, Any?> = mapOf(
            "phase" to command.phase.name,
            "openAt" to now.toString(),
            "closeAt" to now.plusSeconds(command.durationSeconds.toLong()).toString(),
            "secret" to if (mode.isSecret()) generatePin() else null,
            "qrToken" to if (mode.isQr()) generateQrToken() else null,
        )

        @Suppress("UNCHECKED_CAST")
        val updatedWindows = event.validationWindows
            .filter { (it["phase"] as? String) != command.phase.name } + (window as Map<String, Any>)
        event.validationWindows = updatedWindows
        eventRepo.save(event)

        val phaseLabel = if (command.phase == AttendancePhase.ENTRY) "entrada" else "saída"
        return OpenWindowResult(
            message = "Janela de $phaseLabel aberta",
            closeAt = window["closeAt"] as String,
            secret = window["secret"] as String?,
            qrToken = window["qrToken"] as String?,
        )
    }

    fun closeEvent(command: CloseEventCommand): Int {
        val event = eventRepo.findById(command.eventId)
            .orElseThrow { EventNotFoundException(command.eventId) }

        val isOrganizador = event.idOrganizador == command.requestingUserId
        val canManage = command.requestingUserAuthorities.contains("event.manage")
        if (!isOrganizador && !canManage) {
            throw AccessDeniedException("Apenas o organizador pode encerrar o evento")
        }

        event.estado = EventState.CONCLUIDO.name
        eventRepo.save(event)
        return certificateIssuerService.issueCertificatesForEvent(command.eventId)
    }

    private fun generatePin(): String = (100000..999999).random().toString()

    private fun generateQrToken(): String = UUID.randomUUID().toString().replace("-", "")
}
