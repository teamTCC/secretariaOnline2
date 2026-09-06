package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.DiplomaDeliveryResponse
import br.ufpr.sept.so2.modules.iam.api.dto.DiplomaUrlResponse
import br.ufpr.sept.so2.modules.iam.api.dto.GraduationConfirmedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.GraduationRecordResponse
import br.ufpr.sept.so2.modules.iam.api.dto.StudentEligibilityItem
import br.ufpr.sept.so2.modules.iam.application.ConfirmDiplomaDeliveryUseCase
import br.ufpr.sept.so2.modules.iam.application.ConfirmGraduationCommand
import br.ufpr.sept.so2.modules.iam.application.ConfirmGraduationUseCase
import br.ufpr.sept.so2.modules.iam.application.GraduationQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class ConfirmGraduationDto(
    @field:NotEmpty val alunoIds: List<UUID>,
    val idCurso: UUID? = null,
    val dataColacao: LocalDate? = null,
    val observacao: String? = null,
    val livro: String? = null,
    val folha: String? = null,
    val ata: String? = null,
    val periodoId: UUID? = null,
)

@RestController
@Tag(name = "Secretaria — Egressos e Diplomas", description = "Colação de grau e entrega de diploma")
class GraduationController(
    private val graduationQuery: GraduationQuery,
    private val confirmGraduationUseCase: ConfirmGraduationUseCase,
    private val confirmDiplomaDeliveryUseCase: ConfirmDiplomaDeliveryUseCase,
) {
    @GetMapping("/secretaria/egressos")
    @PreAuthorize("hasAuthority('alumni.list') or hasAuthority('system.admin')")
    @Operation(summary = "Listar egressos (format=csv para download)")
    fun listEgressos(
        @RequestParam(required = false) format: String?,
        @RequestParam(required = false) situacaoDiploma: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Any {
        val result = graduationQuery.listEgressos(situacaoDiploma, pageable)
        if (format.equals("csv", ignoreCase = true)) {
            val csv =
                buildString {
                    appendLine("id,nome,email,grr,situacaoDiploma,dataColacao")
                    result.items.forEach { row ->
                        appendLine("${row.id},${row.nome},${row.email},${row.grr ?: ""},${row.situacaoDiploma},${row.dataColacao ?: ""}")
                    }
                }
            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"egressos.csv\"")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csv)
        }
        return PageResponse(
            content = result.items,
            page =
                PageResponse.PageMeta(
                    number = result.number,
                    size = result.size,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                ),
        )
    }

    @GetMapping("/graduations")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('alumni.list') or hasAuthority('system.admin')")
    @Operation(summary = "Listar registros de colação / diplomas")
    fun listGraduations(
        @RequestParam(required = false) estado: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<GraduationRecordResponse> = graduationQuery.listGraduations(estado, pageable)

    @GetMapping("/students")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('user.manage_students') or hasAuthority('system.admin')")
    @Operation(summary = "Listar alunos; eligibleForGraduation=true aplica os critérios de colação")
    fun listStudents(
        @RequestParam(required = false) eligibleForGraduation: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<StudentEligibilityItem> = graduationQuery.listStudents(eligibleForGraduation, pageable)

    @PostMapping("/graduations")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Confirmar colação em lote — atribui role EGRESSO")
    fun confirm(
        @Valid @RequestBody dto: ConfirmGraduationDto,
    ): ResponseEntity<GraduationConfirmedResponse> {
        val result =
            confirmGraduationUseCase.execute(
                ConfirmGraduationCommand(
                    alunoIds = dto.alunoIds,
                    idCurso = dto.idCurso,
                    dataColacao = dto.dataColacao,
                    observacao = dto.observacao,
                    livro = dto.livro,
                    folha = dto.folha,
                    ata = dto.ata,
                    periodoId = dto.periodoId,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            GraduationConfirmedResponse(processados = result.processados, registros = result.registros),
        )
    }

    @PatchMapping("/graduations/{id}/confirm-delivery")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Confirmar entrega física do diploma")
    fun confirmDelivery(
        @PathVariable id: UUID,
    ): ResponseEntity<DiplomaDeliveryResponse> {
        val result = confirmDiplomaDeliveryUseCase.execute(id, currentUserId())
        return ResponseEntity.ok(
            DiplomaDeliveryResponse(id = result.id, estado = result.estado, deliveredAt = result.deliveredAt),
        )
    }

    @GetMapping("/graduations/{id}/diploma-url")
    @PreAuthorize("hasAuthority('diploma.register') or hasAuthority('alumni.list') or hasAuthority('system.admin')")
    @Operation(summary = "URL pré-assinada do PDF do diploma")
    fun diplomaUrl(
        @PathVariable id: UUID,
    ): DiplomaUrlResponse = graduationQuery.diplomaUrl(id)
}
