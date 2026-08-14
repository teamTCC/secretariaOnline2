package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.application.AttachmentInput
import br.ufpr.sept.so2.modules.solicitacoes.application.OpenRequestCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.OpenRequestUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.SaveDraftCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.SaveDraftUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.SubmitDraftCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.SubmitDraftUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.TransitionCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.TransitionRequestUseCase
import br.ufpr.sept.so2.modules.solicitacoes.domain.RequestState
import br.ufpr.sept.so2.modules.solicitacoes.domain.WorkflowDefinition
import br.ufpr.sept.so2.modules.solicitacoes.domain.WorkflowEngine
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
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
import java.util.UUID

data class AttachmentInputDto(
    val storageKey: String,
    val sha256: String,
    val nomeOriginal: String,
    val contentType: String,
    val categoria: String,
    val tamanhoBytes: Long,
)

data class OpenRequestDto(
    val idRequestType: UUID,
    val idCurso: UUID,
    val dados: Map<String, Any>,
    val attachments: List<AttachmentInputDto> = emptyList(),
)

data class TransitionDto(
    @field:NotBlank val action: String,
    val parecer: String?,
)

data class BulkDeliberateDto(
    @field:NotEmpty val ids: List<UUID>,
    @field:NotBlank val action: String,
    val parecer: String? = null,
)

@RestController
@RequestMapping("/requests")
@Tag(name = "Solicitações", description = "Motor de workflow genérico para os 19 tipos de solicitação acadêmica")
class RequestController(
    private val openRequestUseCase: OpenRequestUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val submitDraftUseCase: SubmitDraftUseCase,
    private val transitionUseCase: TransitionRequestUseCase,
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val requestEventRepo: RequestEventJpaRepository,
    private val objectMapper: ObjectMapper,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Abrir nova solicitação")
    fun open(
        @Valid @RequestBody dto: OpenRequestDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        val id =
            openRequestUseCase.execute(
                OpenRequestCommand(
                    idRequestType = dto.idRequestType,
                    idSolicitante = user.userId,
                    idCurso = dto.idCurso,
                    dados = dto.dados,
                    attachments = dto.attachments.map { it.toInput() },
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "id" to id,
                "_links" to mapOf("self" to "/requests/$id"),
            ),
        )
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Salvar rascunho de solicitação")
    fun saveDraft(
        @Valid @RequestBody dto: OpenRequestDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        val id =
            saveDraftUseCase.execute(
                SaveDraftCommand(
                    idRequestType = dto.idRequestType,
                    idSolicitante = user.userId,
                    idCurso = dto.idCurso,
                    dados = dto.dados,
                    attachments = dto.attachments.map { it.toInput() },
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "id" to id,
                "estado" to "RASCUNHO",
                "_links" to
                    mapOf(
                        "self" to "/requests/$id",
                        "submit" to "/requests/$id/submit",
                    ),
            ),
        )
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Submeter rascunho como solicitação oficial")
    fun submitDraft(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        val entity =
            submitDraftUseCase.execute(
                SubmitDraftCommand(
                    requestId = id,
                    idSolicitante = user.userId,
                ),
            )
        val protocolo = "${entity.ano}/${entity.numeroAnual.toString().padStart(4, '0')}"
        return ResponseEntity.ok(
            mapOf(
                "id" to entity.id,
                "estado" to "ABERTA",
                "protocolo" to protocolo,
                "_links" to mapOf("self" to "/requests/$id"),
            ),
        )
    }

    @GetMapping("/{id}/protocol")
    @PreAuthorize("hasAuthority('request.view_own') or hasAuthority('request.view_curso')")
    @Operation(summary = "Informações de protocolo da solicitação")
    fun getProtocol(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val user = currentUser()
        val entity =
            requestRepo.findById(id)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: $id") }
        if (user.authorities.contains("request.view_own") &&
            !user.authorities.contains("request.view_curso") &&
            !user.authorities.contains("request.deliberate")
        ) {
            if (entity.idSolicitante != user.userId) {
                throw AccessDeniedException("Acesso negado ao protocolo da solicitação $id")
            }
        }
        val protocolo = "${entity.ano}/${entity.numeroAnual.toString().padStart(4, '0')}"
        return ResponseEntity.ok(
            mapOf(
                "protocolo" to protocolo,
                "tipo" to entity.requestTypeCode,
                "estado" to entity.estado,
                "idSolicitante" to entity.idSolicitante,
                "createdAt" to entity.createdAt,
                "_links" to
                    mapOf(
                        "self" to "/requests/$id",
                        "public" to "/publico/solicitacoes/${entity.ano}/${entity.numeroAnual}",
                    ),
            ),
        )
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('request.view_own', 'request.view_curso', 'request.deliberate')")
    @Operation(summary = "Listar solicitações com filtros")
    fun list(
        @RequestParam(required = false) estado: String?,
        @RequestParam(required = false) idCurso: UUID?,
        @RequestParam(required = false) typeCode: String?,
        @RequestParam(required = false) type: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val user = currentUser()
        val idSolicitante =
            if (user.authorities.contains("request.view_own") &&
                !user.authorities.contains("request.view_curso") &&
                !user.authorities.contains("request.deliberate")
            ) {
                user.userId
            } else {
                null
            }

        val resolvedType = typeCode ?: type
        val canBulk =
            user.authorities.contains("request.deliberate") ||
                user.authorities.contains("image_authorization.review")
        val page = requestRepo.findWithFilters(estado, idSolicitante, idCurso, resolvedType, pageable)
        return PageResponse.of(page) { r ->
            val links = mutableMapOf<String, Any>("self" to "/requests/${r.id}")
            if (canBulk && r.estado == "ABERTA") {
                links["bulk_deliberate"] = "/requests/bulk-deliberate"
            }
            mapOf(
                "id" to r.id,
                "numeroAnual" to r.numeroAnual,
                "ano" to r.ano,
                "tipoCode" to r.requestTypeCode,
                "estado" to r.estado,
                "prazoEm" to r.prazoEm,
                "_links" to links,
            )
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('request.view_own', 'request.view_curso', 'request.deliberate')")
    @Operation(summary = "Detalhe de uma solicitação com HATEOAS transitions")
    fun getById(
        @PathVariable id: UUID,
    ): EntityModel<Map<String, Any?>> {
        val user = currentUser()
        val entity =
            requestRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: $id") }

        // FGAC ownership check for view_own
        if (user.authorities.contains("request.view_own") &&
            !user.authorities.contains("request.view_curso") &&
            !user.authorities.contains("request.deliberate")
        ) {
            require(entity.idSolicitante == user.userId) { "Acesso negado" }
        }

        val requestType = requestTypeRepo.findById(entity.idRequestType).orElseThrow()
        val workflowDef = objectMapper.convertValue(requestType.workflowJson, WorkflowDefinition::class.java)
        val engine = WorkflowEngine(workflowDef)
        val currentState = RequestState.valueOf(entity.estado)
        val allowedTransitions = engine.allowedTransitions(currentState, user.authorities)

        val response =
            mapOf(
                "id" to entity.id,
                "numeroAnual" to entity.numeroAnual,
                "ano" to entity.ano,
                "tipoCode" to entity.requestTypeCode,
                "tipoDescricao" to requestType.descricao,
                "estado" to entity.estado,
                "dados" to entity.dados,
                "parecer" to entity.parecer,
                "prazoEm" to entity.prazoEm,
                "concludedAt" to entity.concludedAt,
                "createdAt" to entity.createdAt,
            )

        val model = EntityModel.of(response)
        model.add(Link.of("/requests/$id").withSelfRel())

        allowedTransitions.forEach { transition ->
            model.add(
                Link
                    .of("/requests/$id/transitions")
                    .withRel(transition.action.lowercase().replace('_', '-'))
                    .withType("POST"),
            )
        }

        return model
    }

    @PostMapping("/{id}/transitions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aplicar transição de workflow (DEFER, DENY, ASSIGN, etc.)")
    fun transition(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: TransitionDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        transitionUseCase.execute(
            TransitionCommand(
                requestId = id,
                action = dto.action,
                actorId = user.userId,
                actorAuthorities = user.authorities,
                parecer = dto.parecer,
            ),
        )
        return ResponseEntity.ok(mapOf("mensagem" to "Transição '${dto.action}' aplicada com sucesso."))
    }

    @PatchMapping("/bulk-deliberate")
    @PreAuthorize("hasAuthority('request.deliberate') or hasAuthority('image_authorization.review')")
    @Operation(summary = "Deliberar várias solicitações na mesma transação (all-or-nothing)")
    @Transactional
    fun bulkDeliberate(
        @Valid @RequestBody dto: BulkDeliberateDto,
    ): ResponseEntity<Map<String, Any>> {
        val user = currentUser()
        try {
            dto.ids.forEach { id ->
                transitionUseCase.execute(
                    TransitionCommand(
                        requestId = id,
                        action = dto.action,
                        actorId = user.userId,
                        actorAuthorities = user.authorities,
                        parecer = dto.parecer,
                    ),
                )
            }
        } catch (e: Exception) {
            throw org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT,
                "Falha parcial na deliberação em lote: ${e.message}",
                e,
            )
        }
        return ResponseEntity.ok(mapOf("processados" to dto.ids.size, "action" to dto.action))
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyAuthority('request.view_own', 'request.view_curso', 'request.deliberate')")
    @Operation(summary = "Histórico de eventos (trilha de auditoria) de uma solicitação")
    fun events(
        @PathVariable id: UUID,
    ): List<Map<String, Any?>> =
        requestEventRepo.findAllByIdRequestOrderByCreatedAtAsc(id).map { e ->
            mapOf(
                "tipo" to e.tipo,
                "estadoAnterior" to e.estadoAnterior,
                "estadoNovo" to e.estadoNovo,
                "parecer" to e.parecer,
                "createdAt" to e.createdAt,
            )
        }

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar tipos de solicitação ativos com formulário JSON Schema")
    fun listTypes(): List<Map<String, Any?>> =
        requestTypeRepo.findAllByAtivoTrue().map { rt ->
            mapOf(
                "id" to rt.id,
                "code" to rt.code,
                "descricao" to rt.descricao,
                "prazoDias" to rt.prazoDias,
                "formSchema" to rt.formSchema,
            )
        }

    private fun AttachmentInputDto.toInput() =
        AttachmentInput(
            storageKey = storageKey,
            sha256 = sha256,
            nomeOriginal = nomeOriginal,
            contentType = contentType,
            categoria = categoria,
            tamanhoBytes = tamanhoBytes,
        )
}
