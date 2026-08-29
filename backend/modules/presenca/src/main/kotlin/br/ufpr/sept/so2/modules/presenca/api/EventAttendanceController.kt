package br.ufpr.sept.so2.modules.presenca.api

import br.ufpr.sept.so2.modules.presenca.api.dto.AttendanceConfirmedResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.AttendanceSessionResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.ConfirmAttendanceDto
import br.ufpr.sept.so2.modules.presenca.api.dto.CreateEventDto
import br.ufpr.sept.so2.modules.presenca.api.dto.EventClosedResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.EventCreatedResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.EventDetailResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.EventSummaryResponse
import br.ufpr.sept.so2.modules.presenca.api.dto.OpenWindowDto
import br.ufpr.sept.so2.modules.presenca.api.dto.WindowOpenedResponse
import br.ufpr.sept.so2.modules.presenca.application.CloseEventCommand
import br.ufpr.sept.so2.modules.presenca.application.ConfirmAttendanceCommand
import br.ufpr.sept.so2.modules.presenca.application.ConfirmAttendanceUseCase
import br.ufpr.sept.so2.modules.presenca.application.CreateEventCommand
import br.ufpr.sept.so2.modules.presenca.application.CreateEventUseCase
import br.ufpr.sept.so2.modules.presenca.application.EventAttendanceQuery
import br.ufpr.sept.so2.modules.presenca.application.ManageEventUseCase
import br.ufpr.sept.so2.modules.presenca.application.OpenWindowCommand
import br.ufpr.sept.so2.modules.presenca.domain.AttendancePhase
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
import java.util.UUID

@RestController
@RequestMapping("/events")
@Tag(name = "Eventos/Presença", description = "Gestão de eventos com horas formativas e confirmação de presença v4.1")
class EventAttendanceController(
    private val eventAttendanceQuery: EventAttendanceQuery,
    private val createEventUseCase: CreateEventUseCase,
    private val confirmAttendanceUseCase: ConfirmAttendanceUseCase,
    private val manageEventUseCase: ManageEventUseCase,
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
    ): PageResponse<EventSummaryResponse> {
        val user = currentUser()
        return eventAttendanceQuery.list(estado, audience, host, idCurso, user.userId, pageable)
    }

    @PostMapping
    @PreAuthorize("hasAuthority('event.manage')")
    @Operation(summary = "Criar novo evento de presença")
    fun create(
        @Valid @RequestBody dto: CreateEventDto,
    ): ResponseEntity<EventCreatedResponse> {
        val user = currentUser()
        val eventId = createEventUseCase.execute(
            CreateEventCommand(
                titulo = dto.titulo,
                descricao = dto.descricao,
                idCurso = dto.idCurso,
                attendanceMode = dto.attendanceMode,
                chCreditadas = dto.chCreditadas,
                inicioEm = dto.inicioEm,
                fimEm = dto.fimEm,
                idOrganizador = user.userId,
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            EventCreatedResponse(id = eventId, links = mapOf("self" to "/events/$eventId")),
        )
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhe de um evento com HATEOAS links baseados em capabilities")
    fun getById(
        @PathVariable eventId: UUID,
    ): EventDetailResponse {
        val user = currentUser()
        return eventAttendanceQuery.getById(eventId, user.userId, user.authorities)
    }

    @GetMapping("/{eventId}/attendance/session")
    @PreAuthorize("hasAuthority('attendance.check_in')")
    @Operation(summary = "Estado da sessão do aluno autenticado + HATEOAS actions disponíveis")
    fun attendanceSession(
        @PathVariable eventId: UUID,
    ): AttendanceSessionResponse {
        val user = currentUser()
        return eventAttendanceQuery.attendanceSession(eventId, user.userId)
    }

    @PostMapping("/{eventId}/attendance/entry")
    @PreAuthorize("hasAuthority('attendance.check_in')")
    @Operation(summary = "Confirmar entrada (modos SECRET_*)")
    fun confirmEntry(
        @PathVariable eventId: UUID,
        @RequestBody dto: ConfirmAttendanceDto,
    ): ResponseEntity<AttendanceConfirmedResponse> {
        val user = currentUser()
        val message =
            confirmAttendanceUseCase.execute(
                ConfirmAttendanceCommand(
                    eventId = eventId,
                    alunoId = user.userId,
                    phase = AttendancePhase.ENTRY,
                    pin = dto.pin,
                    qrToken = dto.qrToken,
                    deviceUuid = dto.deviceUuid,
                ),
            )
        return ResponseEntity.ok(AttendanceConfirmedResponse(mensagem = message))
    }

    @PostMapping("/{eventId}/attendance/exit")
    @PreAuthorize("hasAuthority('attendance.check_in')")
    @Operation(summary = "Confirmar saída (modos SECRET_DUAL)")
    fun confirmExit(
        @PathVariable eventId: UUID,
        @RequestBody dto: ConfirmAttendanceDto,
    ): ResponseEntity<AttendanceConfirmedResponse> {
        val user = currentUser()
        val message =
            confirmAttendanceUseCase.execute(
                ConfirmAttendanceCommand(
                    eventId = eventId,
                    alunoId = user.userId,
                    phase = AttendancePhase.EXIT,
                    pin = dto.pin,
                    qrToken = dto.qrToken,
                    deviceUuid = dto.deviceUuid,
                ),
            )
        return ResponseEntity.ok(AttendanceConfirmedResponse(mensagem = message))
    }

    @PostMapping("/{eventId}/attendance/windows/entry")
    @PreAuthorize("hasAuthority('event.host')")
    @Operation(summary = "Abrir janela de entrada do evento")
    fun openEntryWindow(
        @PathVariable eventId: UUID,
        @RequestBody(required = false) dto: OpenWindowDto?,
    ): ResponseEntity<WindowOpenedResponse> {
        val user = currentUser()
        val result =
            manageEventUseCase.openWindow(
                OpenWindowCommand(
                    eventId = eventId,
                    phase = AttendancePhase.ENTRY,
                    durationSeconds = dto?.durationSeconds ?: 3600,
                    requestingUserId = user.userId,
                    requestingUserAuthorities = user.authorities,
                ),
            )
        return ResponseEntity.ok(WindowOpenedResponse(mensagem = result.message, closeAt = result.closeAt))
    }

    @PostMapping("/{eventId}/attendance/windows/exit")
    @PreAuthorize("hasAuthority('event.host')")
    @Operation(summary = "Abrir janela de saída do evento (modos DUAL)")
    fun openExitWindow(
        @PathVariable eventId: UUID,
        @RequestBody(required = false) dto: OpenWindowDto?,
    ): ResponseEntity<WindowOpenedResponse> {
        val user = currentUser()
        val result =
            manageEventUseCase.openWindow(
                OpenWindowCommand(
                    eventId = eventId,
                    phase = AttendancePhase.EXIT,
                    durationSeconds = dto?.durationSeconds ?: 3600,
                    requestingUserId = user.userId,
                    requestingUserAuthorities = user.authorities,
                ),
            )
        return ResponseEntity.ok(WindowOpenedResponse(mensagem = result.message, closeAt = result.closeAt))
    }

    @PostMapping("/{eventId}/attendance/qr/validate")
    @PreAuthorize("hasAuthority('attendance.check_in')")
    @Operation(summary = "Validar QR de entrada ou saída conforme o estado da sessão")
    fun validateQr(
        @PathVariable eventId: UUID,
        @RequestBody dto: ConfirmAttendanceDto,
    ): ResponseEntity<AttendanceConfirmedResponse> {
        val user = currentUser()
        val phase = eventAttendanceQuery.resolveQrPhase(eventId, user.userId)
        val message =
            confirmAttendanceUseCase.execute(
                ConfirmAttendanceCommand(
                    eventId = eventId,
                    alunoId = user.userId,
                    phase = phase,
                    pin = dto.pin,
                    qrToken = dto.qrToken,
                    deviceUuid = dto.deviceUuid,
                ),
            )
        return ResponseEntity.ok(AttendanceConfirmedResponse(mensagem = message))
    }

    @PostMapping("/{eventId}/close")
    @PreAuthorize("hasAuthority('event.host')")
    @Operation(summary = "Encerrar evento e emitir certificados")
    fun closeEvent(
        @PathVariable eventId: UUID,
    ): ResponseEntity<EventClosedResponse> {
        val user = currentUser()
        val certCount =
            manageEventUseCase.closeEvent(
                CloseEventCommand(
                    eventId = eventId,
                    requestingUserId = user.userId,
                    requestingUserAuthorities = user.authorities,
                ),
            )
        return ResponseEntity.ok(
            EventClosedResponse(
                mensagem = "Evento encerrado. $certCount certificados emitidos.",
                certificadosEmitidos = certCount,
            ),
        )
    }
}
