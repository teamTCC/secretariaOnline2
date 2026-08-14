package br.ufpr.sept.so2.modules.formativas.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityEntity
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryEntity
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.modules.presenca.application.CertificateIssuerService
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class SubmitFormativaDto(
    @field:NotBlank val titulo: String,
    val descricao: String?,
    @field:NotBlank val categoria: String,
    val cargaHoraria: Double,
    val dataRealizacao: LocalDate,
    val storageKeyComprovante: String? = null,
)

data class GenerateComprovanteUploadUrlDto(
    @field:NotBlank val filename: String,
    @field:NotBlank val contentType: String,
)

data class ReviewFormativaDto(
    @field:NotBlank val acao: String,
    val parecer: String?,
)

@RestController
@RequestMapping("/formativas")
@Tag(name = "Horas Formativas", description = "Submissão e revisão de atividades complementares")
class FormativasController(
    private val activityRepo: FormativeActivityJpaRepository,
    private val entryRepo: FormativeEntryJpaRepository,
    private val outboxRepo: OutboxEventJpaRepository,
    private val minioStorageService: MinioStorageService,
    private val certificateIssuer: CertificateIssuerService,
) {
    @PostMapping("/comprovantes/presigned-url")
    @PreAuthorize("hasAuthority('formative.submit')")
    @Operation(summary = "URL presignada MinIO para comprovante (orphan — vincula na submissão)")
    fun generateComprovanteUrl(
        @Valid @RequestBody dto: GenerateComprovanteUploadUrlDto,
    ): ResponseEntity<Map<String, String>> {
        val storageKey = "formativas/orphan/${UUID.randomUUID()}_${dto.filename}"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, dto.contentType, expiryMinutes = 30)
        return ResponseEntity.ok(mapOf("uploadUrl" to uploadUrl, "storageKey" to storageKey))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('formative.submit')")
    @Operation(summary = "Submeter atividade formativa para aprovação")
    fun submit(
        @Valid @RequestBody dto: SubmitFormativaDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        val entity =
            FormativeActivityEntity(
                idAluno = user.userId,
                titulo = dto.titulo,
                descricao = dto.descricao,
                categoria = dto.categoria,
                cargaHoraria = dto.cargaHoraria,
                dataRealizacao = dto.dataRealizacao,
                storageKeyComprovante = dto.storageKeyComprovante,
            )
        val saved = activityRepo.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "estado" to saved.estado),
        )
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAuthority('formative.view_own')")
    @Operation(summary = "Listar minhas atividades formativas")
    fun listOwn(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val user = currentUser()
        return PageResponse.of(activityRepo.findAllByIdAluno(user.userId, pageable)) { a ->
            mapOf(
                "id" to a.id,
                "titulo" to a.titulo,
                "categoria" to a.categoria,
                "cargaHoraria" to a.cargaHoraria,
                "estado" to a.estado,
                "dataRealizacao" to a.dataRealizacao,
            )
        }
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasAuthority('formative.review')")
    @Operation(summary = "Listar atividades pendentes de revisão (CAAF)")
    fun listPendentes(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(activityRepo.findAllByEstado("PENDENTE", pageable)) { a ->
            mapOf(
                "id" to a.id,
                "idAluno" to a.idAluno,
                "titulo" to a.titulo,
                "categoria" to a.categoria,
                "cargaHoraria" to a.cargaHoraria,
                "dataRealizacao" to a.dataRealizacao,
            )
        }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAuthority('formative.review')")
    @Operation(summary = "Aprovar ou rejeitar atividade formativa (CAAF)")
    @Transactional
    fun review(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: ReviewFormativaDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        val activity = activityRepo.findById(id).orElseThrow { NoSuchElementException("Atividade não encontrada: $id") }
        require(activity.estado == "PENDENTE") { "Atividade não está pendente de revisão." }

        activity.estado =
            when (dto.acao.uppercase()) {
                "APROVAR" -> "APROVADA"
                "REJEITAR" -> "REJEITADA"
                else -> throw IllegalArgumentException("Ação inválida: ${dto.acao}")
            }
        activity.parecerRevisor = dto.parecer
        activity.idRevisor = user.userId
        activityRepo.save(activity)

        if (activity.estado == "APROVADA" && !entryRepo.existsByIdActivity(activity.id)) {
            entryRepo.save(
                FormativeEntryEntity(
                    idAluno = activity.idAluno,
                    idActivity = activity.id,
                    horasAprovadas = activity.cargaHoraria,
                    aprovadoEm = OffsetDateTime.now(),
                ),
            )
            certificateIssuer.issueFormativeCertificate(
                alunoId = activity.idAluno,
                activityId = activity.id,
                titulo = activity.titulo,
                chCreditadas = activity.cargaHoraria,
            )
        }

        outboxRepo.save(
            OutboxEventEntity(
                eventType = "formativas.revisada",
                aggregateType = "FormativeActivity",
                aggregateId = activity.id,
                payload =
                    mapOf(
                        "activityId" to activity.id.toString(),
                        "idAluno" to activity.idAluno.toString(),
                        "estado" to activity.estado,
                        "parecer" to (activity.parecerRevisor ?: ""),
                    ),
            ),
        )

        return ResponseEntity.ok(mapOf("estado" to activity.estado))
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasAuthority('formative.view_own')")
    @Operation(summary = "Resumo de horas formativas do aluno autenticado")
    fun resumo(): Map<String, Any> {
        val user = currentUser()
        val total = entryRepo.sumHorasAprovadas(user.userId)
        return mapOf("horasAprovadas" to total, "horasRequeridas" to 120.0, "percentual" to (total / 120.0 * 100).coerceAtMost(100.0))
    }
}
