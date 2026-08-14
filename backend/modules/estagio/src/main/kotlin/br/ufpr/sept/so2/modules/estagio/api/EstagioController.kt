package br.ufpr.sept.so2.modules.estagio.api

import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipEntity
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class DeclararEstagioDto(
    @field:NotBlank val empresa: String,
    @field:NotBlank val cargo: String,
    @field:Min(1) val cargaHorariaSemanal: Int,
    val inicio: LocalDate,
    val observacoes: String?,
)

data class AtualizarEstagioDto(
    val cargo: String?,
    val cargaHorariaSemanal: Int?,
    val fim: LocalDate?,
    val observacoes: String?,
    val idSupervisor: UUID?,
)

@RestController
@RequestMapping("/internships")
@Tag(name = "Estágios", description = "Gestão de estágios obrigatórios e não-obrigatórios")
class EstagioController(
    private val internshipRepo: InternshipJpaRepository,
    private val outboxRepo: OutboxEventJpaRepository,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('internship.view_own')")
    @Operation(summary = "Declarar início de estágio (ALUNO)")
    @Transactional
    fun declarar(
        @Valid @RequestBody dto: DeclararEstagioDto,
    ): ResponseEntity<Map<String, Any>> {
        val userId = currentUserId()
        val entity =
            InternshipEntity(
                idAluno = userId,
                empresa = dto.empresa,
                cargo = dto.cargo,
                cargaHorariaSemanal = dto.cargaHorariaSemanal,
                inicio = dto.inicio,
                observacoes = dto.observacoes,
            )
        val saved = internshipRepo.save(entity)
        outboxRepo.save(
            OutboxEventEntity(
                eventType = "estagio.declarado",
                aggregateType = "internship",
                aggregateId = saved.id,
                payload =
                    mapOf(
                        "internshipId" to saved.id.toString(),
                        "idAluno" to userId.toString(),
                        "empresa" to dto.empresa,
                    ),
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to saved.id, "estado" to saved.estado))
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('internship.view_own')")
    @Operation(summary = "Listar estágios do aluno autenticado")
    fun mine(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val userId = currentUserId()
        return PageResponse.of(internshipRepo.findAllByIdAluno(userId, pageable)) { it.toSummaryMap() }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('internship.review') or hasAuthority('internship.supervise')")
    @Operation(summary = "Listar estágios — COE vê todos, supervisor vê seus supervisionados")
    fun list(
        @RequestParam(defaultValue = "EM_ANDAMENTO") estado: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val user = currentUser()
        val page =
            if (user.authorities.contains("internship.supervise") && !user.authorities.contains("internship.review")) {
                internshipRepo.findAllByIdSupervisor(user.userId, pageable)
            } else {
                internshipRepo.findAllByEstado(estado, pageable)
            }
        return PageResponse.of(page) { it.toSummaryMap() }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhe de estágio")
    fun get(
        @PathVariable id: UUID,
    ): EntityModel<Map<String, Any?>> {
        val internship = internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        val user = currentUser()
        val isOwner = internship.idAluno == user.userId
        val isSupervisor = internship.idSupervisor == user.userId
        val canReview = user.authorities.contains("internship.review")
        if (!isOwner && !isSupervisor && !canReview) {
            throw AccessDeniedException("Acesso negado ao estágio $id")
        }
        val links = mutableListOf(Link.of("/internships/$id").withSelfRel())
        if (canReview || isSupervisor) links.add(Link.of("/internships/$id").withRel("update"))
        if (canReview) links.add(Link.of("/internships/$id/conclude").withRel("conclude"))
        links.add(Link.of("/internships/$id/documents").withRel("documents"))
        return EntityModel.of(internship.toDetailMap(), links)
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('internship.supervise') or hasAuthority('internship.review')")
    @Operation(summary = "Atualizar dados do estágio (supervisor/COE)")
    @Transactional
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AtualizarEstagioDto,
    ): ResponseEntity<Map<String, Any?>> {
        val internship = internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        dto.cargo?.let { internship.cargo = it }
        dto.cargaHorariaSemanal?.let { internship.cargaHorariaSemanal = it }
        dto.fim?.let { internship.fim = it }
        dto.observacoes?.let { internship.observacoes = it }
        dto.idSupervisor?.let { internship.idSupervisor = it }
        internshipRepo.save(internship)
        return ResponseEntity.ok(internship.toDetailMap())
    }

    @PostMapping("/{id}/conclude")
    @PreAuthorize("hasAuthority('internship.review')")
    @Operation(summary = "Concluir estágio (COE)")
    @Transactional
    fun conclude(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val internship = internshipRepo.findById(id).orElseThrow { NoSuchElementException("Estágio não encontrado: $id") }
        require(internship.estado == "EM_ANDAMENTO") { "Estágio não está EM_ANDAMENTO." }
        internship.estado = "CONCLUIDO"
        if (internship.fim == null) internship.fim = LocalDate.now()
        internshipRepo.save(internship)
        outboxRepo.save(
            OutboxEventEntity(
                eventType = "estagio.concluido",
                aggregateType = "internship",
                aggregateId = internship.id,
                payload =
                    mapOf(
                        "internshipId" to internship.id.toString(),
                        "idAluno" to internship.idAluno.toString(),
                        "empresa" to internship.empresa,
                    ),
            ),
        )
        return ResponseEntity.ok(mapOf("estado" to internship.estado, "fim" to internship.fim.toString()))
    }

    private fun InternshipEntity.toSummaryMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "empresa" to empresa,
            "cargo" to cargo,
            "estado" to estado,
            "inicio" to inicio,
            "fim" to fim,
        )

    private fun InternshipEntity.toDetailMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "idAluno" to idAluno,
            "idSupervisor" to idSupervisor,
            "empresa" to empresa,
            "cargo" to cargo,
            "cargaHorariaSemanal" to cargaHorariaSemanal,
            "estado" to estado,
            "inicio" to inicio,
            "fim" to fim,
            "observacoes" to observacoes,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
        )
}
