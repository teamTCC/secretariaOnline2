package br.ufpr.sept.so2.modules.presenca.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.IamDashboardPort
import br.ufpr.sept.so2.modules.presenca.api.dto.AttendanceSessionResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.EventDetailResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.EventSummaryResponse
import br.ufpr.sept.so2.modules.presenca.domain.AttendanceMode
import br.ufpr.sept.so2.modules.presenca.domain.AttendancePhase
import br.ufpr.sept.so2.modules.presenca.domain.EventState
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.AttendanceSessionJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EventAttendanceQuery(
    private val eventRepo: EventAttendanceJpaRepository,
    private val sessionRepo: AttendanceSessionJpaRepository,
    private val usuarioRepo: IamDashboardPort,
) {
    fun list(
        estado: String?,
        audience: String?,
        host: String?,
        idCurso: UUID?,
        userId: UUID,
        pageable: Pageable,
    ): PageResponse<EventSummaryResponse> {
        val organizadorFilter = if (host == "me") userId else null
        val page =
            if (audience == "me") {
                val cursoIdFromUser = usuarioRepo.findUserCourseId(userId)
                eventRepo.findOpenForAudience(cursoIdFromUser ?: idCurso, pageable)
            } else {
                eventRepo.findWithFilters(estado, organizadorFilter, idCurso, pageable)
            }
        return PageResponse.of(page) { e ->
            EventSummaryResponse(
                id = e.id,
                titulo = e.titulo,
                attendanceMode = e.attendanceMode,
                estado = e.estado,
                chCreditadas = e.chCreditadas,
                inicioEm = e.inicioEm,
                fimEm = e.fimEm,
            )
        }
    }

    fun getById(
        eventId: UUID,
        userId: UUID,
        authorities: Set<String>,
    ): EventDetailResponse {
        val event =
            eventRepo
                .findById(eventId)
                .orElseThrow { NoSuchElementException("Evento não encontrado: $eventId") }

        val links = linkedMapOf("self" to "/events/$eventId")
        val isOrganizador = event.idOrganizador == userId
        val canManage = authorities.contains("event.manage")
        val canHost = authorities.contains("event.host") || isOrganizador
        val estado = EventState.valueOf(event.estado)

        val mode = AttendanceMode.valueOf(event.attendanceMode)
        if (canHost && estado != EventState.CONCLUIDO) {
            links["abrir-janela-entrada"] = "/events/$eventId/attendance/windows/entry"
            if (estado == EventState.EM_ANDAMENTO) {
                if (mode.isDual()) {
                    links["abrir-janela-saida"] = "/events/$eventId/attendance/windows/exit"
                }
                links["encerrar-evento"] = "/events/$eventId/close"
            }
        }

        if ((isOrganizador || canManage) && estado == EventState.AGENDADO) {
            links["editar"] = "/events/$eventId"
        }

        return EventDetailResponse(
            id = event.id,
            titulo = event.titulo,
            descricao = event.descricao,
            attendanceMode = event.attendanceMode,
            estado = event.estado,
            chCreditadas = event.chCreditadas,
            inicioEm = event.inicioEm,
            fimEm = event.fimEm,
            links = links,
        )
    }

    fun attendanceSession(
        eventId: UUID,
        userId: UUID,
    ): AttendanceSessionResponse {
        val event =
            eventRepo
                .findById(eventId)
                .orElseThrow { NoSuchElementException("Evento não encontrado: $eventId") }

        val session = sessionRepo.findByIdEventoAndIdAluno(eventId, userId).orElse(null)
        val mode = AttendanceMode.valueOf(event.attendanceMode)
        val estado = EventState.valueOf(event.estado)

        val sessionComplete =
            session?.let { s ->
                if (mode.isDual()) s.entryConfirmedAt != null && s.exitConfirmedAt != null else s.entryConfirmedAt != null
            } ?: false

        val links = linkedMapOf("self" to "/events/$eventId/attendance/session")
        val isInProgress = estado == EventState.EM_ANDAMENTO
        val entryDone = session?.entryConfirmedAt != null
        val entryWindowActive = isInProgress && hasActiveWindow(event.validationWindows, AttendancePhase.ENTRY)
        val exitWindowActive = isInProgress && hasActiveWindow(event.validationWindows, AttendancePhase.EXIT)

        if (entryWindowActive && !entryDone) {
            if (mode.isQr()) {
                links["confirmar-entrada"] = "/events/$eventId/attendance/qr/validate"
            } else {
                links["confirmar-entrada"] = "/events/$eventId/attendance/entry"
            }
        }

        if (exitWindowActive && mode.isDual() && entryDone && session?.exitConfirmedAt == null) {
            if (mode.isQr()) {
                links["confirmar-saida"] = "/events/$eventId/attendance/qr/validate"
            } else {
                links["confirmar-saida"] = "/events/$eventId/attendance/exit"
            }
        }

        return AttendanceSessionResponse(
            idEvento = eventId,
            estado = event.estado,
            attendanceMode = event.attendanceMode,
            entryConfirmedAt = session?.entryConfirmedAt,
            exitConfirmedAt = session?.exitConfirmedAt,
            isComplete = sessionComplete,
            links = links,
        )
    }

    fun resolveQrPhase(
        eventId: UUID,
        alunoId: UUID,
    ): AttendancePhase {
        val session = sessionRepo.findByIdEventoAndIdAluno(eventId, alunoId).orElse(null)
        return if (session?.entryConfirmedAt == null) AttendancePhase.ENTRY else AttendancePhase.EXIT
    }

    private fun hasActiveWindow(
        windows: List<Map<String, Any>>,
        phase: AttendancePhase,
    ): Boolean {
        val now = java.time.OffsetDateTime.now()
        return windows.any { w ->
            (w["phase"] as? String) == phase.name &&
                runCatching {
                    java.time.OffsetDateTime.parse(w["openAt"] as String).isBefore(now) &&
                        java.time.OffsetDateTime.parse(w["closeAt"] as String).isAfter(now)
                }.getOrDefault(false)
        }
    }
}
