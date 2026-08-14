package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FaqItemEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FaqItemJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SupportTicketEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SupportTicketJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUserId
import br.ufpr.sept.so2.shared.security.hasAuthority
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateTicketDto(
    @field:NotBlank val assunto: String,
    @field:NotBlank val descricao: String,
)

data class RespondTicketDto(
    @field:NotBlank val resposta: String,
)

data class CreateFaqItemDto(
    @field:NotBlank val categoria: String,
    @field:NotBlank val pergunta: String,
    @field:NotBlank val resposta: String,
    val ordem: Int = 0,
)

data class UpdateFaqItemDto(
    val pergunta: String?,
    val resposta: String?,
    val ordem: Int?,
    val ativo: Boolean?,
)

@RestController
@Tag(name = "FAQ e Suporte", description = "Base de conhecimento pública e tickets de suporte")
class SupportController(
    private val faqRepo: FaqItemJpaRepository,
    private val ticketRepo: SupportTicketJpaRepository,
) {
    // ─── FAQ (público, sem autenticação) ──────────────────────────────────────

    @GetMapping("/faq")
    @SecurityRequirements
    @Operation(summary = "Listar FAQ — público, sem necessidade de login")
    fun listFaq(
        @RequestParam(required = false) categoria: String?,
    ): List<Map<String, Any?>> {
        val items =
            if (categoria != null) {
                faqRepo.findAllByCategoriaAndAtivoOrderByOrdemAsc(categoria, true)
            } else {
                faqRepo.findAllByAtivoOrderByOrdemAsc(true)
            }
        return items.map { f ->
            mapOf(
                "id" to f.id,
                "categoria" to f.categoria,
                "pergunta" to f.pergunta,
                "resposta" to f.resposta,
                "ordem" to f.ordem,
            )
        }
    }

    // ─── FAQ Admin CRUD ───────────────────────────────────────────────────────

    @PostMapping("/faq")
    @PreAuthorize("hasAuthority('system.admin')")
    @Operation(summary = "Criar item de FAQ (Admin)")
    fun createFaqItem(
        @Valid @RequestBody dto: CreateFaqItemDto,
    ): ResponseEntity<Map<String, Any?>> {
        val entity = FaqItemEntity(
            categoria = dto.categoria,
            pergunta = dto.pergunta,
            resposta = dto.resposta,
            ordem = dto.ordem,
        )
        val saved = faqRepo.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "id" to saved.id,
                "categoria" to saved.categoria,
                "pergunta" to saved.pergunta,
                "ordem" to saved.ordem,
            ),
        )
    }

    @PatchMapping("/faq/{id}")
    @PreAuthorize("hasAuthority('system.admin')")
    @Operation(summary = "Atualizar item de FAQ (Admin)")
    fun updateFaqItem(
        @PathVariable id: UUID,
        @RequestBody dto: UpdateFaqItemDto,
    ): ResponseEntity<Map<String, Any?>> {
        val item = faqRepo.findById(id).orElseThrow { NoSuchElementException("FAQ item não encontrado: $id") }
        dto.pergunta?.let { item.pergunta = it }
        dto.resposta?.let { item.resposta = it }
        dto.ordem?.let { item.ordem = it }
        dto.ativo?.let { item.ativo = it }
        val saved = faqRepo.save(item)
        return ResponseEntity.ok(
            mapOf(
                "id" to saved.id,
                "categoria" to saved.categoria,
                "pergunta" to saved.pergunta,
                "resposta" to saved.resposta,
                "ordem" to saved.ordem,
                "ativo" to saved.ativo,
            ),
        )
    }

    @DeleteMapping("/faq/{id}")
    @PreAuthorize("hasAuthority('system.admin')")
    @Operation(summary = "Desativar item de FAQ — soft delete (Admin)")
    fun deleteFaqItem(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val item = faqRepo.findById(id).orElseThrow { NoSuchElementException("FAQ item não encontrado: $id") }
        item.ativo = false
        faqRepo.save(item)
        return ResponseEntity.noContent().build()
    }

    // ─── Support Tickets ──────────────────────────────────────────────────────

    @PostMapping("/support/tickets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Abrir ticket de suporte (Aluno ou qualquer usuário autenticado)")
    fun createTicket(
        @Valid @RequestBody dto: CreateTicketDto,
    ): ResponseEntity<Map<String, Any?>> {
        val entity =
            SupportTicketEntity(
                idUsuario = currentUserId(),
                assunto = dto.assunto,
                descricao = dto.descricao,
            )
        val saved = ticketRepo.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "estado" to saved.estado, "assunto" to saved.assunto),
        )
    }

    @GetMapping("/support/tickets/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Meus tickets de suporte")
    fun myTickets(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(ticketRepo.findAllByIdUsuario(currentUserId(), pageable)) { t ->
            mapOf(
                "id" to t.id,
                "assunto" to t.assunto,
                "estado" to t.estado,
                "resposta" to t.resposta,
                "createdAt" to t.createdAt,
            )
        }

    @GetMapping("/support/tickets")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Listar todos os tickets (Secretaria / Admin) com filtro por estado")
    fun listAll(
        @RequestParam(required = false) estado: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val page =
            if (estado != null) {
                ticketRepo.findAllByEstado(estado.uppercase(), pageable)
            } else {
                ticketRepo.findAll(pageable)
            }
        return PageResponse.of(page) { t ->
            mapOf(
                "id" to t.id,
                "idUsuario" to t.idUsuario,
                "assunto" to t.assunto,
                "estado" to t.estado,
                "idAtendente" to t.idAtendente,
                "createdAt" to t.createdAt,
            )
        }
    }

    @PatchMapping("/support/tickets/{id}/respond")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Responder ticket (Secretaria)")
    fun respond(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RespondTicketDto,
    ): ResponseEntity<Map<String, Any?>> {
        val ticket = ticketRepo.findById(id).orElseThrow { NoSuchElementException("Ticket não encontrado: $id") }
        require(ticket.estado != "FECHADO") { "Ticket já está fechado." }

        ticket.resposta = dto.resposta
        ticket.estado = "RESPONDIDO"
        ticket.idAtendente = currentUserId()
        ticketRepo.save(ticket)

        return ResponseEntity.ok(mapOf("id" to ticket.id, "estado" to ticket.estado))
    }

    @PatchMapping("/support/tickets/{id}/close")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fechar ticket (Secretaria ou o próprio usuário que abriu)")
    fun close(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val currentId = currentUserId()
        val ticket = ticketRepo.findById(id).orElseThrow { NoSuchElementException("Ticket não encontrado: $id") }
        require(ticket.estado != "FECHADO") { "Ticket já está fechado." }

        val isOwner = ticket.idUsuario == currentId
        val isStaff = hasAuthority("user.manage_students") || hasAuthority("system.admin")
        if (!isOwner && !isStaff) {
            throw AccessDeniedException("Apenas o autor do ticket ou a secretaria podem fechá-lo.")
        }

        ticket.estado = "FECHADO"
        ticketRepo.save(ticket)

        return ResponseEntity.ok(mapOf("id" to ticket.id, "estado" to ticket.estado))
    }
}
