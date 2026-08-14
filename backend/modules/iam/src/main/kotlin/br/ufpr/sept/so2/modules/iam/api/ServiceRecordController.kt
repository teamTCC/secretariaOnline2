package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ServiceRecordEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ServiceRecordJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
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
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
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
    private val serviceRecordRepo: ServiceRecordJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val auditPublisher: AuditPublisher,
) {
    @PostMapping("/service-records")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Registrar atendimento de aluno (Secretaria)")
    @Transactional
    fun create(
        @Valid @RequestBody dto: CreateServiceRecordDto,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, Any?>> {
        val entity =
            ServiceRecordEntity(
                idSecretario = currentUserId(),
                idAluno = dto.idAluno,
                tipo = dto.tipo,
                assunto = dto.assunto,
                descricao = dto.descricao,
                estado = "PENDENTE_CIENCIA",
            )
        val saved = serviceRecordRepo.save(entity)
        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.ATENDIMENTO_CRIADO,
            aggregateType = "ServiceRecord",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "alunoId" to saved.idAluno.toString(),
                    "assunto" to saved.assunto,
                    "tipo" to saved.tipo,
                ),
        )
        auditPublisher.publish(
            AuditPayload(
                acao = "SERVICE_RECORD_CREATED",
                idAtor = currentUserId(),
                alvoTipo = "service_record",
                alvoId = saved.id,
                ip = clientIp(httpRequest),
                userAgent = httpRequest.getHeader("User-Agent"),
                resultado = "OK",
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toMap(includeAcknowledge = false))
    }

    @GetMapping("/service-records")
    @PreAuthorize("hasAuthority('user.manage_students') or hasAuthority('service_record.view_own')")
    @Operation(summary = "Listar atendimentos — secretaria ou aluno=me")
    fun list(
        @RequestParam(required = false) idAluno: UUID?,
        @RequestParam(required = false) aluno: String?,
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val user = currentUser()
        val ownOnly =
            aluno.equals("me", ignoreCase = true) ||
                (
                    user.authorities.contains("service_record.view_own") &&
                        !user.authorities.contains("user.manage_students")
                )
        if (ownOnly && !user.authorities.contains("service_record.view_own") &&
            !user.authorities.contains("user.manage_students")
        ) {
            throw AccessDeniedException("Capability service_record.view_own ausente.")
        }
        val targetAluno = if (ownOnly) user.userId else idAluno
        val estado = status?.uppercase()
        val page =
            when {
                targetAluno != null && estado != null ->
                    serviceRecordRepo.findAllByIdAlunoAndEstado(targetAluno, estado, pageable)
                targetAluno != null -> serviceRecordRepo.findAllByIdAluno(targetAluno, pageable)
                else -> {
                    if (!user.authorities.contains("user.manage_students")) {
                        throw AccessDeniedException("Listagem geral exige user.manage_students.")
                    }
                    serviceRecordRepo.findAll(pageable)
                }
            }
        return PageResponse.of(page) { r -> r.toMap(includeAcknowledge = ownOnly) }
    }

    @GetMapping("/service-records/aluno/{id}")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Histórico de atendimentos de um aluno específico (Secretaria)")
    fun historyByAluno(
        @PathVariable id: UUID,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(serviceRecordRepo.findAllByIdAluno(id, pageable)) { r ->
            r.toMap(includeAcknowledge = false)
        }

    @GetMapping("/me/service-records")
    @PreAuthorize("hasAuthority('service_record.view_own') or hasAuthority('user.update_own_profile')")
    @Operation(summary = "Meu histórico de atendimentos (Aluno autenticado)")
    fun myHistory(
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val alunoId = currentUserId()
        val page =
            if (status != null) {
                serviceRecordRepo.findAllByIdAlunoAndEstado(alunoId, status.uppercase(), pageable)
            } else {
                serviceRecordRepo.findAllByIdAluno(alunoId, pageable)
            }
        return PageResponse.of(page) { r -> r.toMap(includeAcknowledge = true) }
    }

    @PostMapping("/me/service-records")
    @PreAuthorize("hasAuthority('service_record.view_own')")
    @Operation(summary = "Aluno agenda atendimento (tipo AGENDAMENTO)")
    @Transactional
    fun schedule(
        @Valid @RequestBody dto: ScheduleServiceRecordDto,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, Any?>> {
        val alunoId = currentUserId()
        val entity =
            ServiceRecordEntity(
                idSecretario = null,
                idAluno = alunoId,
                tipo = dto.tipo.ifBlank { "AGENDAMENTO" },
                assunto = dto.assunto,
                descricao = dto.descricao,
                estado = "AGENDADO",
            )
        val saved = serviceRecordRepo.save(entity)
        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.ATENDIMENTO_CRIADO,
            aggregateType = "ServiceRecord",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "alunoId" to saved.idAluno.toString(),
                    "assunto" to saved.assunto,
                    "tipo" to saved.tipo,
                ),
        )
        auditPublisher.publish(
            AuditPayload(
                acao = "SERVICE_RECORD_SCHEDULED",
                idAtor = alunoId,
                alvoTipo = "service_record",
                alvoId = saved.id,
                ip = clientIp(httpRequest),
                userAgent = httpRequest.getHeader("User-Agent"),
                resultado = "OK",
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toMap(includeAcknowledge = false))
    }

    @PostMapping("/service-records/{id}/acknowledge")
    @PreAuthorize("hasAuthority('service_record.view_own')")
    @Operation(summary = "Aluno dá ciência no atendimento")
    @Transactional
    fun acknowledge(
        @PathVariable id: UUID,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, Any?>> {
        val rec =
            serviceRecordRepo.findById(id).orElseThrow { NoSuchElementException("Atendimento não encontrado: $id") }
        if (rec.idAluno != currentUserId()) {
            throw AccessDeniedException("Acesso negado ao atendimento $id")
        }
        require(rec.estado == "PENDENTE_CIENCIA") { "Atendimento já possui ciência (estado=${rec.estado})." }
        rec.estado = "CIENTE"
        rec.acknowledgedAt = OffsetDateTime.now()
        serviceRecordRepo.save(rec)
        auditPublisher.publish(
            AuditPayload(
                acao = "SERVICE_RECORD_ACKNOWLEDGED",
                idAtor = currentUserId(),
                alvoTipo = "service_record",
                alvoId = rec.id,
                ip = clientIp(httpRequest),
                userAgent = httpRequest.getHeader("User-Agent"),
                resultado = "OK",
            ),
        )
        return ResponseEntity.ok(rec.toMap(includeAcknowledge = false))
    }

    private fun ServiceRecordEntity.toMap(includeAcknowledge: Boolean): Map<String, Any?> {
        val links = mutableMapOf<String, Any>("self" to "/service-records/$id")
        if (includeAcknowledge && estado == "PENDENTE_CIENCIA") {
            links["acknowledge"] = "/service-records/$id/acknowledge"
        }
        return mapOf(
            "id" to id,
            "idAluno" to idAluno,
            "idSecretario" to idSecretario,
            "assunto" to assunto,
            "tipo" to tipo,
            "descricao" to descricao,
            "estado" to estado,
            "status" to estado,
            "acknowledgedAt" to acknowledgedAt,
            "createdAt" to createdAt,
            "_links" to links,
        )
    }

    private fun clientIp(request: HttpServletRequest): String? =
        request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim() ?: request.remoteAddr
}
