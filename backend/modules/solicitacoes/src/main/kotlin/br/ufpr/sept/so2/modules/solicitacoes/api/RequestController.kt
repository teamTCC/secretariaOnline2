package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.application.OpenRequestCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.OpenRequestUseCase
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
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class OpenRequestDto(
    val idRequestType: UUID,
    val idCurso: UUID,
    val dados: Map<String, Any>,
)

data class TransitionDto(
    @field:NotBlank val action: String,
    val parecer: String?,
)

@RestController
@RequestMapping("/requests")
@Tag(name = "Solicitações", description = "Motor de workflow genérico para os 19 tipos de solicitação acadêmica")
class RequestController(
    private val openRequestUseCase: OpenRequestUseCase,
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
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "id" to id,
                "_links" to mapOf("self" to "/requests/$id"),
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

        val page = requestRepo.findWithFilters(estado, idSolicitante, idCurso, typeCode, pageable)
        return PageResponse.of(page) { r ->
            mapOf(
                "id" to r.id,
                "numeroAnual" to r.numeroAnual,
                "ano" to r.ano,
                "tipoCode" to r.requestTypeCode,
                "estado" to r.estado,
                "prazoEm" to r.prazoEm,
                "_links" to mapOf("self" to "/requests/${r.id}"),
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
}
