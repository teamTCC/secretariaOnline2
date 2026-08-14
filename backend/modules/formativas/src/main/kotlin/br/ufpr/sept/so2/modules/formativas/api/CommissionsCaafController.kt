package br.ufpr.sept.so2.modules.formativas.api

import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryEntity
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
import br.ufpr.sept.so2.modules.presenca.application.CertificateIssuerService
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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
import java.util.UUID

data class BatchReviewDto(
    @field:NotEmpty val ids: List<UUID>,
    @field:NotBlank val acao: String,
    val parecer: String? = null,
)

@RestController
@RequestMapping("/commissions/caaf")
@Tag(name = "Comissão CAAF", description = "Pool de revisão de atividades formativas para membros da CAAF")
@PreAuthorize("hasAuthority('formative.review')")
class CommissionsCaafController(
    private val activityRepo: FormativeActivityJpaRepository,
    private val entryRepo: FormativeEntryJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val certificateIssuer: CertificateIssuerService,
) {
    @GetMapping("/pool")
    @Operation(summary = "Pool CAAF — atividades pendentes sem revisor atribuído")
    fun pool(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(activityRepo.findAllByEstadoAndIdRevisorIsNull("PENDENTE", pageable)) { a ->
            mapOf(
                "id" to a.id,
                "idAluno" to a.idAluno,
                "titulo" to a.titulo,
                "categoria" to a.categoria,
                "cargaHoraria" to a.cargaHoraria,
                "dataRealizacao" to a.dataRealizacao,
            )
        }

    @PostMapping("/{activityId}/claim")
    @Operation(summary = "Reivindicar revisão — CAAF membro auto-atribui a atividade")
    @Transactional
    fun claim(
        @PathVariable activityId: UUID,
    ): ResponseEntity<Map<String, Any>> {
        val activity =
            activityRepo
                .findById(activityId)
                .orElseThrow { NoSuchElementException("Atividade não encontrada: $activityId") }
        require(activity.estado == "PENDENTE") { "Atividade não está pendente: ${activity.estado}" }
        require(activity.idRevisor == null) { "Atividade já foi reivindicada por outro membro." }

        activity.idRevisor = currentUserId()
        activityRepo.save(activity)

        return ResponseEntity.ok(mapOf<String, Any>("id" to activity.id, "idRevisor" to (activity.idRevisor ?: "")))
    }

    @PostMapping("/batch-review")
    @Operation(summary = "Revisão em lote — aprovar ou rejeitar múltiplas atividades de uma vez")
    @Transactional
    fun batchReview(
        @Valid @RequestBody dto: BatchReviewDto,
    ): ResponseEntity<Map<String, Any>> {
        val revisorId = currentUserId()
        val novoEstado =
            when (dto.acao.uppercase()) {
                "APROVAR" -> "APROVADA"
                "REJEITAR" -> "REJEITADA"
                else -> throw IllegalArgumentException("Ação inválida: ${dto.acao}. Use APROVAR ou REJEITAR.")
            }

        val activities = activityRepo.findAllById(dto.ids)
        activities.forEach { a ->
            val wasPending = a.estado == "PENDENTE"
            a.estado = novoEstado
            a.parecerRevisor = dto.parecer
            a.idRevisor = revisorId
            if (novoEstado == "APROVADA" && wasPending) {
                entryRepo.save(
                    FormativeEntryEntity(
                        idAluno = a.idAluno,
                        idActivity = a.id,
                        horasAprovadas = a.cargaHoraria,
                        aprovadoEm = OffsetDateTime.now(),
                    ),
                )
            }
            if (novoEstado == "APROVADA" && wasPending) {
                certificateIssuer.issueFormativeCertificate(
                    alunoId = a.idAluno,
                    activityId = a.id,
                    titulo = a.titulo,
                    chCreditadas = a.cargaHoraria,
                )
            }
        }
        activityRepo.saveAll(activities)

        outboxPublisher.enqueue(
            eventType = "formativas.batch_revisada",
            aggregateType = "FormativeActivity",
            aggregateId = revisorId,
            payload =
                mapOf(
                    "ids" to dto.ids.map { it.toString() },
                    "acao" to dto.acao.uppercase(),
                    "novoEstado" to novoEstado,
                    "revisorId" to revisorId.toString(),
                    "parecer" to (dto.parecer ?: ""),
                ),
        )

        return ResponseEntity.ok(
            mapOf(
                "processadas" to activities.size,
                "estado" to novoEstado,
            ),
        )
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas da CAAF — pendentes, aprovadas hoje, taxa de aprovação")
    fun stats(): Map<String, Any> {
        val startOfDay = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC)
        val totalPendente = activityRepo.countByEstado("PENDENTE")
        val aprovadasHoje = activityRepo.countByEstadoAndUpdatedAtAfter("APROVADA", startOfDay)
        val rejeitadasHoje = activityRepo.countByEstadoAndUpdatedAtAfter("REJEITADA", startOfDay)

        return mapOf(
            "totalPendente" to totalPendente,
            "aprovadasHoje" to aprovadasHoje,
            "rejeitadasHoje" to rejeitadasHoje,
        )
    }
}
