package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.ServiceRecordResponse
import br.ufpr.sept.so2.modules.iam.application.AcknowledgeServiceRecordCommand
import br.ufpr.sept.so2.modules.iam.application.CreateServiceRecordCommand
import br.ufpr.sept.so2.modules.iam.application.ScheduleServiceRecordCommand
import br.ufpr.sept.so2.modules.iam.application.ServiceRecordQuery
import br.ufpr.sept.so2.modules.iam.application.ServiceRecordUseCase
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateServiceRecordDto(
    val idAluno: UUID,
    @field:NotBlank val assunto: String,
    val descricao: String? = null,
    val tipo: String = "PRESENCIAL",
)

data class ScheduleServiceRecordDto(
    @field:NotBlank val assunto: String,
    val descricao: String? = null,
    val tipo: String = "AGENDAMENTO",
)

@RestController
@Tag(name = "Atendimentos", description = "Registro de atendimentos presenciais ou virtuais pela secretaria")
class ServiceRecordController(
    private val serviceRecordQuery: ServiceRecordQuery,
    private val serviceRecordUseCase: ServiceRecordUseCase,
) {
    @PostMapping("/service-records")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Registrar atendimento de aluno (Secretaria)")
    fun create(
        @Valid @RequestBody dto: CreateServiceRecordDto,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ServiceRecordResponse> {
        val saved =
            serviceRecordUseCase.create(
                CreateServiceRecordCommand(
                    idAluno = dto.idAluno,
                    assunto = dto.assunto,
                    descricao = dto.descricao,
                    tipo = dto.tipo,
                    idSecretario = currentUserId(),
                    clientIp = clientIp(httpRequest),
                    userAgent = httpRequest.getHeader("User-Agent"),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @GetMapping("/service-records")
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('service_record.view_own')")
    @Operation(summary = "Listar atendimentos — secretaria ou aluno=me")
    fun list(
        @RequestParam(required = false) idAluno: UUID?,
        @RequestParam(required = false) aluno: String?,
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<ServiceRecordResponse> = serviceRecordQuery.list(currentUser(), idAluno, aluno, status, pageable)

    @GetMapping("/service-records/aluno/{id}")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Histórico de atendimentos de um aluno específico (Secretaria)")
    fun historyByAluno(
        @PathVariable id: UUID,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<ServiceRecordResponse> = serviceRecordQuery.historyByAluno(id, pageable)

    @GetMapping("/me/service-records")
    @PreAuthorize("hasAuthority('service_record.view_own') or hasAuthority('user.update_own_profile')")
    @Operation(summary = "Meu histórico de atendimentos (Aluno autenticado)")
    fun myHistory(
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<ServiceRecordResponse> = serviceRecordQuery.myHistory(currentUserId(), status, pageable)

    @PostMapping("/me/service-records")
    @PreAuthorize("hasAuthority('service_record.view_own')")
    @Operation(summary = "Aluno agenda atendimento (tipo AGENDAMENTO)")
    fun schedule(
        @Valid @RequestBody dto: ScheduleServiceRecordDto,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ServiceRecordResponse> {
        val saved =
            serviceRecordUseCase.schedule(
                ScheduleServiceRecordCommand(
                    idAluno = currentUserId(),
                    assunto = dto.assunto,
                    descricao = dto.descricao,
                    tipo = dto.tipo,
                    clientIp = clientIp(httpRequest),
                    userAgent = httpRequest.getHeader("User-Agent"),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PostMapping("/service-records/{id}/acknowledge")
    @PreAuthorize("hasAuthority('service_record.view_own')")
    @Operation(summary = "Aluno dá ciência no atendimento")
    fun acknowledge(
        @PathVariable id: UUID,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ServiceRecordResponse> {
        val rec =
            serviceRecordUseCase.acknowledge(
                AcknowledgeServiceRecordCommand(
                    recordId = id,
                    alunoId = currentUserId(),
                    clientIp = clientIp(httpRequest),
                    userAgent = httpRequest.getHeader("User-Agent"),
                ),
            )
        return ResponseEntity.ok(rec)
    }

    private fun clientIp(request: HttpServletRequest): String? =
        request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim() ?: request.remoteAddr
}
