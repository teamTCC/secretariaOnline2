package br.ufpr.sept.so2.modules.presenca.application

import br.ufpr.sept.so2.modules.presenca.domain.AttendanceException
import br.ufpr.sept.so2.modules.presenca.domain.AttendanceMode
import br.ufpr.sept.so2.modules.presenca.domain.AttendancePhase
import br.ufpr.sept.so2.modules.presenca.domain.EventNotFoundException
import br.ufpr.sept.so2.modules.presenca.domain.EventState
import br.ufpr.sept.so2.modules.presenca.domain.InvalidAttendanceTokenException
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.AttendanceSessionEntity
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.AttendanceSessionJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class ConfirmAttendanceCommand(
    val eventId: UUID,
    val alunoId: UUID,
    val phase: AttendancePhase,
    val pin: String?,
    val qrToken: String?,
    val deviceUuid: String?,
)

@Service
@Transactional
class ConfirmAttendanceUseCase(
    private val eventRepo: EventAttendanceJpaRepository,
    private val sessionRepo: AttendanceSessionJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun execute(command: ConfirmAttendanceCommand): String {
        val event = eventRepo.findById(command.eventId)
            .orElseThrow { EventNotFoundException(command.eventId) }

        if (event.estado != EventState.EM_ANDAMENTO.name) {
            throw AttendanceException("Evento não está em andamento.")
        }

        val mode = AttendanceMode.valueOf(event.attendanceMode)

        val activeWindow = event.validationWindows.firstOrNull { w ->
            (w["phase"] as? String) == command.phase.name &&
                OffsetDateTime.parse(w["openAt"] as String).isBefore(OffsetDateTime.now()) &&
                OffsetDateTime.parse(w["closeAt"] as String).isAfter(OffsetDateTime.now())
        } ?: throw AttendanceException("Janela de ${command.phase} não está ativa.")

        if (mode.isSecret()) {
            val expectedPin = activeWindow["secret"] as? String
                ?: throw AttendanceException("PIN não configurado para esta janela.")
            if (command.pin != expectedPin) throw InvalidAttendanceTokenException("PIN inválido.")
        }

        if (mode.isQr()) {
            val expectedToken = activeWindow["qrToken"] as? String
                ?: throw AttendanceException("QR token não configurado.")
            if (command.qrToken != expectedToken) throw InvalidAttendanceTokenException("Token QR inválido.")
        }

        if (command.deviceUuid != null && command.phase == AttendancePhase.ENTRY) {
            if (sessionRepo.existsByIdEventoAndDeviceUuid(command.eventId, command.deviceUuid)) {
                throw AttendanceException("Este dispositivo já foi utilizado para confirmar presença neste evento.")
            }
        }

        val session = sessionRepo.findByIdEventoAndIdAluno(command.eventId, command.alunoId)
            .orElseGet {
                sessionRepo.save(
                    AttendanceSessionEntity(
                        idEvento = command.eventId,
                        idAluno = command.alunoId,
                        deviceUuid = command.deviceUuid,
                    ),
                )
            }

        val now = OffsetDateTime.now()
        when (command.phase) {
            AttendancePhase.ENTRY -> {
                if (session.entryConfirmedAt != null) throw AttendanceException("Entrada já confirmada.")
                sessionRepo.confirmEntry(session.id, now)
            }
            AttendancePhase.EXIT -> {
                if (session.entryConfirmedAt == null) throw AttendanceException("Entrada ainda não confirmada.")
                if (session.exitConfirmedAt != null) throw AttendanceException("Saída já confirmada.")
                sessionRepo.confirmExit(session.id, now)
            }
        }

        outboxPublisher.enqueue(
            eventType = "presenca.confirmada",
            aggregateType = "AttendanceSession",
            aggregateId = session.id,
            payload = mapOf(
                "eventId" to command.eventId.toString(),
                "alunoId" to command.alunoId.toString(),
                "phase" to command.phase.name,
            ),
        )

        val phaseLabel = command.phase.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$phaseLabel confirmada com sucesso."
    }
}
