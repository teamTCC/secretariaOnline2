package br.ufpr.sept.so2.modules.comunicacao.api

import br.ufpr.sept.so2.modules.comunicacao.api.dto.CommunicationDeliveryResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.CommunicationDetailResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.CommunicationPublishedResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.CommunicationSummaryResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.PublishCommunicationDto
import br.ufpr.sept.so2.modules.comunicacao.api.dto.UnreadCountResponse
import br.ufpr.sept.so2.modules.comunicacao.application.CommunicationsQuery
import br.ufpr.sept.so2.modules.comunicacao.application.MarkDeliveryReadCommand
import br.ufpr.sept.so2.modules.comunicacao.application.MarkDeliveryReadUseCase
import br.ufpr.sept.so2.modules.comunicacao.application.PublishCommunicationCommand
import br.ufpr.sept.so2.modules.comunicacao.application.PublishCommunicationUseCase
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
import java.util.UUID

@RestController
@RequestMapping("/communications")
@Tag(name = "Comunicados", description = "Hub de comunicações institucionais e inbox in-app")
class CommunicationsController(
    private val communicationsQuery: CommunicationsQuery,
    private val markDeliveryReadUseCase: MarkDeliveryReadUseCase,
    private val publishCommunicationUseCase: PublishCommunicationUseCase,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Listar comunicados publicados (mais recentes primeiro)")
    fun listPublished(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<CommunicationSummaryResponse> = communicationsQuery.listPublished(pageable)

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Inbox do usuário autenticado (deliveries in-app)")
    fun myInbox(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<CommunicationDeliveryResponse> = communicationsQuery.myInbox(currentUserId(), pageable)

    @GetMapping("/me/unread-count")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Contador de comunicados não lidos (badge)")
    fun unreadCount(): UnreadCountResponse = communicationsQuery.unreadCount(currentUserId())

    @PatchMapping("/deliveries/{deliveryId}/read")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Marcar delivery como lido")
    fun markRead(
        @PathVariable deliveryId: UUID,
    ): ResponseEntity<Void> {
        markDeliveryReadUseCase.execute(
            MarkDeliveryReadCommand(deliveryId = deliveryId, userId = currentUserId()),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping
    @PreAuthorize("hasAuthority('communication.publish') or hasAuthority('communication.publish_class')")
    @Operation(summary = "Publicar comunicado institucional")
    fun publish(
        @Valid @RequestBody dto: PublishCommunicationDto,
    ): ResponseEntity<CommunicationPublishedResponse> {
        val user = currentUser()
        val result =
            publishCommunicationUseCase.execute(
                PublishCommunicationCommand(
                    idAutor = user.userId,
                    titulo = dto.titulo,
                    conteudo = dto.conteudo,
                    tipo = dto.tipo,
                    cursoId = dto.cursoId,
                    isAdmin = user.authorities.contains("communication.publish"),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CommunicationPublishedResponse(id = result.id, entregas = result.entregas),
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Detalhe de um comunicado publicado")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<CommunicationDetailResponse> = ResponseEntity.ok(communicationsQuery.getById(id))
}
