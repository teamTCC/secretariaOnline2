package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.CreateFaqItemDto
import br.ufpr.sept.so2.modules.iam.api.dto.CreateTicketDto
import br.ufpr.sept.so2.modules.iam.api.dto.FaqCreatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.FaqItemResponse
import br.ufpr.sept.so2.modules.iam.api.dto.FaqUpdatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.RespondTicketDto
import br.ufpr.sept.so2.modules.iam.api.dto.TicketAdminSummaryResponse
import br.ufpr.sept.so2.modules.iam.api.dto.TicketCreatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.TicketStateResponse
import br.ufpr.sept.so2.modules.iam.api.dto.TicketSummaryResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UpdateFaqItemDto
import br.ufpr.sept.so2.modules.iam.application.CreateFaqItemCommand
import br.ufpr.sept.so2.modules.iam.application.CreateTicketCommand
import br.ufpr.sept.so2.modules.iam.application.ManageSupportUseCase
import br.ufpr.sept.so2.modules.iam.application.SupportQuery
import br.ufpr.sept.so2.modules.iam.application.UpdateFaqItemCommand
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

@RestController
@Tag(name = "FAQ e Suporte", description = "Base de conhecimento pública e tickets de suporte")
class SupportController(
    private val supportQuery: SupportQuery,
    private val manageSupportUseCase: ManageSupportUseCase,
) {
    // ─── FAQ (público, sem autenticação) ──────────────────────────────────────

    @GetMapping("/faq")
    @SecurityRequirements
    @Operation(summary = "Listar FAQ — público, sem necessidade de login")
    fun listFaq(
        @RequestParam(required = false) categoria: String?,
    ): List<FaqItemResponse> = supportQuery.listFaq(categoria)

    // ─── FAQ Admin CRUD ───────────────────────────────────────────────────────

    @PostMapping("/faq")
    @PreAuthorize("hasAuthority('system.admin')")
    @Operation(summary = "Criar item de FAQ (Admin)")
    fun createFaqItem(
        @Valid @RequestBody dto: CreateFaqItemDto,
    ): ResponseEntity<FaqCreatedResponse> {
        val created =
            manageSupportUseCase.createFaqItem(
                CreateFaqItemCommand(
                    categoria = dto.categoria,
                    pergunta = dto.pergunta,
                    resposta = dto.resposta,
                    ordem = dto.ordem,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PatchMapping("/faq/{id}")
    @PreAuthorize("hasAuthority('system.admin')")
    @Operation(summary = "Atualizar item de FAQ (Admin)")
    fun updateFaqItem(
        @PathVariable id: UUID,
        @RequestBody dto: UpdateFaqItemDto,
    ): ResponseEntity<FaqUpdatedResponse> {
        val updated =
            manageSupportUseCase.updateFaqItem(
                UpdateFaqItemCommand(
                    id = id,
                    pergunta = dto.pergunta,
                    resposta = dto.resposta,
                    ordem = dto.ordem,
                    ativo = dto.ativo,
                ),
            )
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/faq/{id}")
    @PreAuthorize("hasAuthority('system.admin')")
    @Operation(summary = "Desativar item de FAQ — soft delete (Admin)")
    fun deleteFaqItem(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        manageSupportUseCase.deleteFaqItem(id)
        return ResponseEntity.noContent().build()
    }

    // ─── Support Tickets ──────────────────────────────────────────────────────

    @PostMapping("/support/tickets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Abrir ticket de suporte (Aluno ou qualquer usuário autenticado)")
    fun createTicket(
        @Valid @RequestBody dto: CreateTicketDto,
    ): ResponseEntity<TicketCreatedResponse> {
        val created =
            manageSupportUseCase.createTicket(
                CreateTicketCommand(
                    userId = currentUserId(),
                    assunto = dto.assunto,
                    descricao = dto.descricao,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/support/tickets/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Meus tickets de suporte")
    fun myTickets(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<TicketSummaryResponse> = supportQuery.myTickets(currentUserId(), pageable)

    @GetMapping("/support/tickets")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Listar todos os tickets (Secretaria / Admin) com filtro por estado")
    fun listAll(
        @RequestParam(required = false) estado: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<TicketAdminSummaryResponse> = supportQuery.listAll(estado, pageable)

    @PatchMapping("/support/tickets/{id}/respond")
    @PreAuthorize("hasAuthority('user.manage_students')")
    @Operation(summary = "Responder ticket (Secretaria)")
    fun respond(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RespondTicketDto,
    ): ResponseEntity<TicketStateResponse> =
        ResponseEntity.ok(manageSupportUseCase.respond(id, dto.resposta, currentUserId()))

    @PatchMapping("/support/tickets/{id}/close")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fechar ticket (Secretaria ou o próprio usuário que abriu)")
    fun close(
        @PathVariable id: UUID,
    ): ResponseEntity<TicketStateResponse> = ResponseEntity.ok(manageSupportUseCase.close(id, currentUser()))
}
