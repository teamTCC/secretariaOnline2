package br.ufpr.sept.so2.modules.formativas.api

import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityEntity
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class SubmitFormativaDto(
    @field:NotBlank val titulo: String,
    val descricao: String?,
    @field:NotBlank val categoria: String,
    val cargaHoraria: Double,
    val dataRealizacao: LocalDate,
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
) {
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
