package br.ufpr.sept.so2.modules.presenca.api

import br.ufpr.sept.so2.modules.presenca.domain.AttendanceMode
import br.ufpr.sept.so2.modules.presenca.domain.AttendancePhase
import br.ufpr.sept.so2.modules.presenca.domain.EventState
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.AttendanceSessionEntity
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.AttendanceSessionJpaRepository
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceEntity
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.EventAttendanceJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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

@RestController
@RequestMapping("/events")
@Tag(name = "Eventos/Presença", description = "Gestão de eventos com horas formativas e confirmação de presença v4.1")
class EventAttendanceController(
    private val eventRepo: EventAttendanceJpaRepository,
    private val sessionRepo: AttendanceSessionJpaRepository,
    private val objectMapper: ObjectMapper,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar eventos com filtros (audiência, organizador, estado)")
    fun list(
        @RequestParam(required = false) estado: String?,
        @RequestParam(required = false) audience: String?,
        @RequestParam(required = false) host: String?,
        @RequestParam(required = false) idCurso: UUID?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val user = currentUser()
        val organizadorFilter = if (host == "me") user.userId else null

        val page = eventRepo.findWithFilters(estado, organizadorFilter, idCurso, pageable)
        return PageResponse.of(page) { e ->
            mapOf(
                "id" to e.id,
                "titulo" to e.titulo,
                "attendanceMode" to e.attendanceMode,
                "estado" to e.estado,
                "chCreditadas" to e.chCreditadas,
                "inicioEm" to e.inicioEm,
                "fimEm" to e.fimEm,
            )
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('event.manage')")
    @Operation(summary = "Criar novo evento de presença")
    fun create(
        @Valid @RequestBody dto: CreateEventDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        val entity =
            EventAttendanceEntity(
                titulo = dto.titulo,
                descricao = dto.descricao,
                idOrganizador = user.userId,
                idCurso = dto.idCurso,
                attendanceMode = dto.attendanceMode.name,
                chCreditadas = dto.chCreditadas,
                inicioEm = dto.inicioEm,
                fimEm = dto.fimEm,
            )
        val saved = eventRepo.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf<String, Any>("id" to saved.id, "_links" to mapOf("self" to "/events/${saved.id}")),
        )
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhe de um evento com HATEOAS links baseados em capabilities")
    fun getById(
        @PathVariable eventId: UUID,
    ): EntityModel<Map<String, Any?>> {
        val user = currentUser()
        val event =
            eventRepo
                .findById(eventId)
                .orElseThrow { NoSuchElementException("Evento não encontrado: $eventId") }

        val response: Map<String, Any?> =
            mapOf(
                "id" to event.id,
                "titulo" to event.titulo,
                "descricao" to event.descricao,
                "attendanceMode" to event.attendanceMode,
                "estado" to event.estado,
                "chCreditadas" to event.chCreditadas,
                "inicioEm" to event.inicioEm,
                "fimEm" to event.fimEm,
            )

        val model = EntityModel.of(response)
        model.add(Link.of("/events/$eventId").withSelfRel())

        val isOrganizador = event.idOrganizador == user.userId
        val canManage = user.authorities.contains("event.manage")
        val canHost = user.authorities.contains("event.host") || isOrganizador
        val estado = EventState.valueOf(event.estado)

        if (canHost && estado == EventState.EM_ANDAMENTO) {
            model.add(Link.of("/events/$eventId/attendance/windows/entry").withRel("abrir-janela-entrada").withType("POST"))
            val mode = AttendanceMode.valueOf(event.attendanceMode)
            if (mode.isDual()) {
                model.add(Link.of("/events/$eventId/attendance/windows/exit").withRel("abrir-janela-saida").withType("POST"))
            }
            model.add(Link.of("/events/$eventId/close").withRel("encerrar-evento").withType("POST"))
        }

        if (isOrganizador || (canManage && estado == EventState.AGENDADO)) {
            model.add(Link.of("/events/$eventId").withRel("editar").withType("PATCH"))
        }

        return model
    }

    @GetMapping("/{eventId}/attendance/session")
    @PreAuthorize("hasAuthority('attendance.check_in')")
    @Operation(summary = "Estado da sessão do aluno autenticado + HATEOAS actions disponíveis")
    fun attendanceSession(
        @PathVariable eventId: UUID,
    ): EntityModel<Map<String, Any?>> {
        val user = currentUser()
        val event =
            eventRepo
                .findById(eventId)
                .orElseThrow { NoSuchElementException("Evento não encontrado: $eventId") }

        val session = sessionRepo.findByIdEventoAndIdAluno(eventId, user.userId).orElse(null)
        val mode = AttendanceMode.valueOf(event.attendanceMode)
        val estado = EventState.valueOf(event.estado)

        val response: Map<String, Any?> =
            mapOf(
                "idEvento" to eventId,
                "estado" to event.estado,
                "attendanceMode" to event.attendanceMode,
                "entryConfirmedAt" to session?.entryConfirmedAt,
                "exitConfirmedAt" to session?.exitConfirmedAt,
                "isComplete" to (session?.isComplete(mode) ?: false),
            )

        val model = EntityModel.of(response)
        model.add(Link.of("/events/$eventId/attendance/session").withSelfRel())

        val isInProgress = estado == EventState.EM_ANDAMENTO
        val entryDone = session?.hasEntryConfirmed() == true

        if (isInProgress && !entryDone) {
            if (mode.isQr()) {
                model.add(Link.of("/events/$eventId/attendance/qr/validate").withRel("confirmar-entrada").withType("POST"))
            } else {
                model.add(Link.of("/events/$eventId/attendance/entry").withRel("confirmar-entrada").withType("POST"))
            }
        }

        if (isInProgress && mode.isDual() && entryDone && session?.exitConfirmedAt == null) {
            if (mode.isQr()) {
                model.add(Link.of("/events/$eventId/attendance/qr/validate").withRel("confirmar-saida").withType("POST"))
            } else {
                model.add(Link.of("/events/$eventId/attendance/exit").withRel("confirmar-saida").withType("POST"))
            }
        }

        return model
    }

    @PostMapping("/{eventId}/attendance/entry")
    @PreAuthorize("hasAuthority('attendance.check_in')")
    @Operation(summary = "Confirmar entrada (modos SECRET_*)")
    fun confirmEntry(
        @PathVariable eventId: UUID,
        @RequestBody dto: ConfirmAttendanceDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        return processAttendance(eventId, user.userId, AttendancePhase.ENTRY, dto)
    }

    @PostMapping("/{eventId}/attendance/exit")
    @PreAuthorize("hasAuthority('attendance.check_in')")
    @Operation(summary = "Confirmar saída (modos SECRET_DUAL)")
    fun confirmExit(
        @PathVariable eventId: UUID,
        @RequestBody dto: ConfirmAttendanceDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        return processAttendance(eventId, user.userId, AttendancePhase.EXIT, dto)
    }

    @PostMapping("/{eventId}/attendance/windows/entry")
    @PreAuthorize("hasAuthority('event.host')")
    @Operation(summary = "Abrir janela de entrada do evento")
    fun openEntryWindow(
        @PathVariable eventId: UUID,
        @RequestBody(required = false) dto: OpenWindowDto?,
    ): ResponseEntity<Map<String, Any>> {
        val event = eventRepo.findById(eventId).orElseThrow { NoSuchElementException("Evento não encontrado: $eventId") }
        val user = currentUser()
        require(
            event.idOrganizador == user.userId || user.authorities.contains("event.manage"),
        ) { "Apenas o organizador pode abrir janelas" }

        if (event.estado == "AGENDADO") {
            eventRepo.updateEstado(eventId, EventState.EM_ANDAMENTO.name)
        }

        val durationSecs = dto?.durationSeconds ?: 3600
        val now = OffsetDateTime.now()
        val window: Map<String, Any?> =
            mapOf(
                "phase" to "ENTRY",
                "openAt" to now.toString(),
                "closeAt" to now.plusSeconds(durationSecs.toLong()).toString(),
                "secret" to if (AttendanceMode.valueOf(event.attendanceMode).isSecret()) generatePin() else null,
                "qrToken" to if (AttendanceMode.valueOf(event.attendanceMode).isQr()) generateQrToken() else null,
            )

        @Suppress("UNCHECKED_CAST")
        val updatedWindows = event.validationWindows.filter { (it["phase"] as? String) != "ENTRY" } + (window as Map<String, Any>)
        event.validationWindows = updatedWindows
        eventRepo.save(event)

        return ResponseEntity.ok(mapOf<String, Any>("mensagem" to "Janela de entrada aberta", "closeAt" to (window["closeAt"] ?: "")))
    }

    @PostMapping("/{eventId}/close")
    @PreAuthorize("hasAuthority('event.host')")
    @Operation(summary = "Encerrar evento e emitir certificados")
    fun closeEvent(
        @PathVariable eventId: UUID,
    ): ResponseEntity<Map<String, Any>> {
        val event = eventRepo.findById(eventId).orElseThrow { NoSuchElementException("Evento não encontrado: $eventId") }
        val user = currentUser()
        require(
            event.idOrganizador == user.userId || user.authorities.contains("event.manage"),
        ) { "Apenas o organizador pode encerrar o evento" }

        eventRepo.updateEstado(eventId, EventState.CONCLUIDO.name)
        return ResponseEntity.ok(mapOf("mensagem" to "Evento encerrado. Certificados sendo processados."))
    }

    private fun processAttendance(
        eventId: UUID,
        alunoId: UUID,
        phase: AttendancePhase,
        dto: ConfirmAttendanceDto,
    ): ResponseEntity<Map<String, Any>> {
        val event =
            eventRepo
                .findById(eventId)
                .orElseThrow { NoSuchElementException("Evento não encontrado: $eventId") }

        require(event.estado == EventState.EM_ANDAMENTO.name) {
            "Evento não está em andamento."
        }

        val mode = AttendanceMode.valueOf(event.attendanceMode)
        val activeWindow =
            event.validationWindows.firstOrNull { w ->
                (w["phase"] as? String) == phase.name &&
                    OffsetDateTime.parse(w["openAt"] as String).isBefore(OffsetDateTime.now()) &&
                    OffsetDateTime.parse(w["closeAt"] as String).isAfter(OffsetDateTime.now())
            } ?: throw IllegalStateException("Janela de $phase não está ativa.")

        if (mode.isSecret()) {
            val expectedPin =
                activeWindow["secret"] as? String
                    ?: throw IllegalStateException("PIN não configurado para esta janela.")
            require(dto.pin == expectedPin) { "PIN inválido." }
        }

        if (mode.isQr()) {
            val expectedToken =
                activeWindow["qrToken"] as? String
                    ?: throw IllegalStateException("QR token não configurado.")
            require(dto.qrToken == expectedToken) { "Token QR inválido." }
        }

        if (dto.deviceUuid != null && phase == AttendancePhase.ENTRY) {
            val deviceConflict = sessionRepo.existsByIdEventoAndDeviceUuid(eventId, dto.deviceUuid)
            require(!deviceConflict) { "Este dispositivo já foi utilizado para confirmar presença neste evento." }
        }

        val session =
            sessionRepo.findByIdEventoAndIdAluno(eventId, alunoId).orElseGet {
                sessionRepo.save(
                    AttendanceSessionEntity(
                        idEvento = eventId,
                        idAluno = alunoId,
                        deviceUuid = dto.deviceUuid,
                    ),
                )
            }

        val now = OffsetDateTime.now()
        when (phase) {
            AttendancePhase.ENTRY -> {
                require(session.entryConfirmedAt == null) { "Entrada já confirmada." }
                sessionRepo.confirmEntry(session.id, now)
            }
            AttendancePhase.EXIT -> {
                require(session.entryConfirmedAt != null) { "Entrada ainda não confirmada." }
                require(session.exitConfirmedAt == null) { "Saída já confirmada." }
                sessionRepo.confirmExit(session.id, now)
            }
        }

        return ResponseEntity.ok(
            mapOf("mensagem" to "${phase.name.lowercase().replaceFirstChar { it.uppercase() }} confirmada com sucesso."),
        )
    }

    private fun generatePin(): String = (100000..999999).random().toString()

    private fun generateQrToken(): String = UUID.randomUUID().toString().replace("-", "")

    private fun AttendanceSessionEntity.isComplete(mode: AttendanceMode): Boolean =
        if (mode.isDual()) entryConfirmedAt != null && exitConfirmedAt != null else entryConfirmedAt != null

    private fun AttendanceSessionEntity.hasEntryConfirmed(): Boolean = entryConfirmedAt != null
}
