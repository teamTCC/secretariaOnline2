package br.ufpr.sept.so2.modules.estagio.api

import br.ufpr.sept.so2.modules.estagio.api.dto.AssignSupervisorDto
import br.ufpr.sept.so2.modules.estagio.api.dto.AssignSupervisorResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.BulkAssignResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.BulkAssignSupervisorDto
import br.ufpr.sept.so2.modules.estagio.api.dto.CoePoolItemResponse
import br.ufpr.sept.so2.modules.estagio.api.dto.CoeStatsResponse
import br.ufpr.sept.so2.modules.estagio.application.AssignSupervisorCommand
import br.ufpr.sept.so2.modules.estagio.application.AssignSupervisorUseCase
import br.ufpr.sept.so2.modules.estagio.application.BulkAssignSupervisorCommand
import br.ufpr.sept.so2.modules.estagio.application.CommissionsCoeQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/commissions/coe")
@Tag(name = "Comissão COE", description = "Pool de supervisão de estágios para membros da COE")
@PreAuthorize("hasAuthority('internship.review')")
class CommissionsCoeController(
    private val commissionsCoeQuery: CommissionsCoeQuery,
    private val assignSupervisorUseCase: AssignSupervisorUseCase,
) {
    @GetMapping("/pool")
    @Operation(summary = "Pool COE — estágios em andamento sem supervisor atribuído")
    fun pool(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<CoePoolItemResponse> = commissionsCoeQuery.pool(pageable)

    @PostMapping("/{internshipId}/assign-supervisor")
    @Operation(summary = "Atribuir supervisor a um estágio")
    fun assignSupervisor(
        @PathVariable internshipId: UUID,
        @Valid @RequestBody dto: AssignSupervisorDto,
    ): ResponseEntity<AssignSupervisorResponse> {
        val result =
            assignSupervisorUseCase.assignSupervisor(
                AssignSupervisorCommand(
                    internshipId = internshipId,
                    idSupervisor = dto.idSupervisor,
                ),
            )
        return ResponseEntity.ok(AssignSupervisorResponse(id = result.id, idSupervisor = result.idSupervisor))
    }

    @PostMapping("/bulk-assign")
    @Operation(summary = "Atribuir supervisor em lote a vários estágios")
    fun bulkAssign(
        @Valid @RequestBody dto: BulkAssignSupervisorDto,
    ): ResponseEntity<BulkAssignResponse> {
        val processados =
            assignSupervisorUseCase.bulkAssign(
                BulkAssignSupervisorCommand(
                    internshipIds = dto.internshipIds,
                    idSupervisor = dto.idSupervisor,
                ),
            )
        return ResponseEntity.ok(BulkAssignResponse(processados = processados))
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas COE — sem supervisor, atribuídos este mês")
    fun stats(): CoeStatsResponse = commissionsCoeQuery.stats()
}
