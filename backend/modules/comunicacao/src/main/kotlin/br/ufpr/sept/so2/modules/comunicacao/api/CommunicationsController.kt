package br.ufpr.sept.so2.modules.comunicacao.api

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationDeliveryJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
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
import java.time.OffsetDateTime
import java.util.UUID

data class PublishCommunicationDto(
    @field:NotBlank val titulo: String,
    @field:NotBlank val conteudo: String,
    @field:NotBlank @field:Pattern(regexp = "AVISO|URGENTE|INFORMATIVO") val tipo: String,
    val cursoId: UUID?,
)

@RestController
@RequestMapping("/communications")
@Tag(name = "Comunicados", description = "Hub de comunicações institucionais e inbox in-app")
class CommunicationsController(
    private val communicationRepo: CommunicationJpaRepository,
    private val deliveryRepo: CommunicationDeliveryJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
) {

    @GetMapping
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Listar comunicados publicados (mais recentes primeiro)")
    fun listPublished(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> =
        PageResponse.of(communicationRepo.findAllByPublishedAtIsNotNullOrderByPublishedAtDesc(pageable)) { c ->
            mapOf(
                "id" to c.id,
                "titulo" to c.titulo,
                "tipo" to c.tipo,
                "publishedAt" to c.publishedAt,
                "audiencia" to c.audiencia,
            )
        }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Inbox do usuário autenticado (deliveries in-app)")
    fun myInbox(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val userId = currentUserId()
        return PageResponse.of(deliveryRepo.findAllByIdUsuarioOrderByDeliveredAtDesc(userId, pageable)) { d ->
            mapOf(
                "deliveryId" to d.id,
                "idCommunication" to d.idCommunication,
                "canal" to d.canal,
                "status" to d.status,
                "deliveredAt" to d.deliveredAt,
                "readAt" to d.readAt,
            )
        }
    }

    @GetMapping("/me/unread-count")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Contador de comunicados não lidos (badge)")
    fun unreadCount(): Map<String, Long> {
        val userId = currentUserId()
        return mapOf("unread" to deliveryRepo.countByIdUsuarioAndReadAtIsNull(userId))
    }

    @PatchMapping("/deliveries/{deliveryId}/read")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Marcar delivery como lido")
    fun markRead(
        @PathVariable deliveryId: UUID,
    ): ResponseEntity<Void> {
        val userId = currentUserId()
        val delivery =
            deliveryRepo
                .findById(deliveryId)
                .orElseThrow { NoSuchElementException("Delivery não encontrado: $deliveryId") }

        require(delivery.idUsuario == userId) { "Acesso negado." }

        deliveryRepo.markRead(deliveryId, OffsetDateTime.now())
        return ResponseEntity.noContent().build()
    }

    @PostMapping
    @PreAuthorize("hasAuthority('communication.publish') or hasAuthority('communication.publish_class')")
    @Operation(summary = "Publicar comunicado institucional")
    fun publish(
        @Valid @RequestBody dto: PublishCommunicationDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        val now = OffsetDateTime.now()

        val audiencia: Map<String, Any> =
            if (user.authorities.contains("communication.publish")) {
                emptyMap()
            } else {
                val cursoId = requireNotNull(dto.cursoId) { "cursoId obrigatório para publicação de turma." }
                mapOf("cursoId" to cursoId.toString())
            }

        val communication =
            communicationRepo.save(
                CommunicationEntity(
                    idAutor = user.userId,
                    titulo = dto.titulo,
                    conteudo = dto.conteudo,
                    tipo = dto.tipo,
                    audiencia = audiencia,
                    publishedAt = now,
                ),
            )

        val cursoId = audiencia["cursoId"]?.toString()
        val targets =
            usuarioRepo.findAll().filter { u ->
                u.ativo && (cursoId == null || u.metadata["idCurso"]?.toString() == cursoId)
            }
        val deliveries =
            targets.map { u ->
                CommunicationDeliveryEntity(
                    idCommunication = communication.id,
                    idUsuario = u.id,
                    canal = "IN_APP",
                    status = "ENTREGUE",
                    deliveredAt = now,
                )
            }
        deliveryRepo.saveAll(deliveries)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf<String, Any>("id" to communication.id, "entregas" to deliveries.size),
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('communication.read')")
    @Operation(summary = "Detalhe de um comunicado publicado")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val communication =
            communicationRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Comunicado não encontrado: $id") }

        require(communication.publishedAt != null) { "Comunicado não publicado." }

        return ResponseEntity.ok(
            mapOf(
                "id" to communication.id,
                "titulo" to communication.titulo,
                "conteudo" to communication.conteudo,
                "tipo" to communication.tipo,
                "audiencia" to communication.audiencia,
                "publishedAt" to communication.publishedAt,
                "expiresAt" to communication.expiresAt,
            ),
        )
    }
}
