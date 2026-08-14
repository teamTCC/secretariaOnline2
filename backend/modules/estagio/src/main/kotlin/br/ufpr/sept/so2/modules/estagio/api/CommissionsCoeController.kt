package br.ufpr.sept.so2.modules.estagio.api

import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class AssignSupervisorDto(val idSupervisor: UUID)

data class BulkAssignSupervisorDto(
    @field:NotEmpty val internshipIds: List<UUID>,
    val idSupervisor: UUID,
)

@RestController
@RequestMapping("/commissions/coe")
@Tag(name = "Comissão COE", description = "Pool de supervisão de estágios para membros da COE")
@PreAuthorize("hasAuthority('internship.review')")
class CommissionsCoeController(
    private val internshipRepo: InternshipJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    @GetMapping("/pool")
    @Operation(summary = "Pool COE — estágios em andamento sem supervisor atribuído")
    fun pool(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(internshipRepo.findAllByEstadoAndIdSupervisorIsNull("EM_ANDAMENTO", pageable)) { i ->
            mapOf(
                "id" to i.id,
                "idAluno" to i.idAluno,
                "empresa" to i.empresa,
                "cargo" to i.cargo,
                "cargaHorariaSemanal" to i.cargaHorariaSemanal,
                "inicio" to i.inicio,
            )
        }

    @PostMapping("/{internshipId}/assign-supervisor")
    @Operation(summary = "Atribuir supervisor a um estágio")
    @Transactional
    fun assignSupervisor(
        @PathVariable internshipId: UUID,
        @Valid @RequestBody dto: AssignSupervisorDto,
    ): ResponseEntity<Map<String, Any?>> {
        val internship =
            internshipRepo
                .findById(internshipId)
                .orElseThrow { NoSuchElementException("Estágio não encontrado: $internshipId") }

        internship.idSupervisor = dto.idSupervisor
        internshipRepo.save(internship)

        outboxPublisher.enqueue(
            eventType = "estagio.supervisor_atribuido",
            aggregateType = "Internship",
            aggregateId = internshipId,
            payload =
                mapOf(
                    "internshipId" to internshipId.toString(),
                    "idSupervisor" to dto.idSupervisor.toString(),
                    "idAluno" to internship.idAluno.toString(),
                ),
        )

        return ResponseEntity.ok(
            mapOf(
                "id" to internship.id,
                "idSupervisor" to internship.idSupervisor,
            ),
        )
    }

    @PostMapping("/bulk-assign")
    @Operation(summary = "Atribuir supervisor em lote a vários estágios")
    @Transactional
    fun bulkAssign(
        @Valid @RequestBody dto: BulkAssignSupervisorDto,
    ): ResponseEntity<Map<String, Any>> {
        val internships = internshipRepo.findAllById(dto.internshipIds)
        internships.forEach { i ->
            i.idSupervisor = dto.idSupervisor
        }
        internshipRepo.saveAll(internships)

        internships.forEach { i ->
            outboxPublisher.enqueue(
                eventType = "estagio.supervisor_atribuido",
                aggregateType = "Internship",
                aggregateId = i.id,
                payload =
                    mapOf(
                        "internshipId" to i.id.toString(),
                        "idSupervisor" to dto.idSupervisor.toString(),
                        "idAluno" to i.idAluno.toString(),
                    ),
            )
        }

        return ResponseEntity.ok(mapOf("processados" to internships.size))
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas COE — sem supervisor, atribuídos este mês")
    fun stats(): Map<String, Any> {
        val startOfMonth =
            OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)

        val semSupervisor = internshipRepo.countByIdSupervisorIsNull()
        val atribuidosEsteMes = internshipRepo.countBySupervisorAssignedAfter(startOfMonth)

        return mapOf(
            "semSupervisor" to semSupervisor,
            "atribuidosEsteMes" to atribuidosEsteMes,
        )
    }
}
