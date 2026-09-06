package br.ufpr.sept.so2.modules.formativas.api

import br.ufpr.sept.so2.modules.formativas.api.dto.BatchReviewDto
import br.ufpr.sept.so2.modules.formativas.api.dto.BatchReviewResultResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.CaafStatsResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaClaimedResponse
import br.ufpr.sept.so2.modules.formativas.api.dto.FormativaPendenteResponse
import br.ufpr.sept.so2.modules.formativas.application.BatchReviewFormativaCommand
import br.ufpr.sept.so2.modules.formativas.application.BatchReviewFormativaUseCase
import br.ufpr.sept.so2.modules.formativas.application.ClaimFormativaCommand
import br.ufpr.sept.so2.modules.formativas.application.ClaimFormativaUseCase
import br.ufpr.sept.so2.modules.formativas.application.CommissionsCaafQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUserId
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
@RequestMapping("/commissions/caaf")
@Tag(name = "Comissão CAAF", description = "Pool de revisão de atividades formativas para membros da CAAF")
@PreAuthorize("hasAuthority('formative.review')")
class CommissionsCaafController(
    private val commissionsCaafQuery: CommissionsCaafQuery,
    private val claimFormativaUseCase: ClaimFormativaUseCase,
    private val batchReviewFormativaUseCase: BatchReviewFormativaUseCase,
) {
    @GetMapping("/pool")
    @Operation(summary = "Pool CAAF — atividades pendentes sem revisor atribuído")
    fun pool(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<FormativaPendenteResponse> = commissionsCaafQuery.pool(pageable)

    @PostMapping("/{activityId}/claim")
    @Operation(summary = "Reivindicar revisão — CAAF membro auto-atribui a atividade")
    fun claim(
        @PathVariable activityId: UUID,
    ): ResponseEntity<FormativaClaimedResponse> {
        val result =
            claimFormativaUseCase.execute(
                ClaimFormativaCommand(activityId = activityId, revisorId = currentUserId()),
            )
        return ResponseEntity.ok(FormativaClaimedResponse(id = result.id, idRevisor = result.idRevisor))
    }

    @PostMapping("/batch-review")
    @Operation(summary = "Revisão em lote — aprovar ou rejeitar múltiplas atividades de uma vez")
    fun batchReview(
        @Valid @RequestBody dto: BatchReviewDto,
    ): ResponseEntity<BatchReviewResultResponse> {
        val result =
            batchReviewFormativaUseCase.execute(
                BatchReviewFormativaCommand(
                    ids = dto.ids,
                    acao = dto.acao,
                    parecer = dto.parecer,
                    revisorId = currentUserId(),
                ),
            )
        return ResponseEntity.ok(
            BatchReviewResultResponse(processadas = result.processadas, estado = result.estado),
        )
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas da CAAF — pendentes, aprovadas hoje, taxa de aprovação")
    fun stats(): CaafStatsResponse = commissionsCaafQuery.stats()
}
