package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.api.dto.AttachmentInputDto
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.BulkDeliberateDto
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.BulkDeliberateResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.DraftCreatedResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.DraftSubmittedResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.OpenRequestDto
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestCreatedResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestDetailResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestEventResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestProtocolResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestSummaryResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestTypeDetailResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestTypeSummaryResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.TransitionAppliedResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.TransitionDto
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.UpdateDraftDto
import br.ufpr.sept.so2.modules.solicitacoes.application.AttachmentInput
import br.ufpr.sept.so2.modules.solicitacoes.application.BulkDeliberateCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.BulkDeliberateUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.OpenRequestCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.OpenRequestUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.RequestQuery
import br.ufpr.sept.so2.modules.solicitacoes.application.SaveDraftCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.SaveDraftUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.SubmitDraftCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.SubmitDraftUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.TransitionCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.TransitionRequestUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.UpdateDraftCommand
import br.ufpr.sept.so2.modules.solicitacoes.application.UpdateDraftUseCase
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.currentUser
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/requests")
@Tag(name = "Solicitações", description = "Motor de workflow genérico para os 19 tipos de solicitação acadêmica")
class RequestController(
    private val openRequestUseCase: OpenRequestUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val submitDraftUseCase: SubmitDraftUseCase,
    private val updateDraftUseCase: UpdateDraftUseCase,
    private val transitionUseCase: TransitionRequestUseCase,
    private val bulkDeliberateUseCase: BulkDeliberateUseCase,
    private val requestQuery: RequestQuery,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('request.open') or hasAuthority('request.open_on_behalf')")
    @Operation(summary = "Abrir nova solicitação (ou em nome de aluno com request.open_on_behalf)")
    fun open(
        @Valid @RequestBody dto: OpenRequestDto,
    ): ResponseEntity<RequestCreatedResponse> {
        val user = currentUser()
        val onBehalf = dto.idSolicitanteOnBehalf?.also {
            require(user.authorities.contains("request.open_on_behalf")) {
                "Você não tem autoridade para abrir solicitações em nome de outro usuário."
            }
        }
        val id =
            openRequestUseCase.execute(
                OpenRequestCommand(
                    idRequestType = dto.idRequestType,
                    idSolicitante = user.userId,
                    idCurso = dto.idCurso,
                    dados = dto.dados,
                    attachments = dto.attachments.map { it.toInput() },
                    onBehalfOfId = onBehalf,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            RequestCreatedResponse(
                id = id,
                links = mapOf("self" to "/requests/$id"),
            ),
        )
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Salvar rascunho de solicitação")
    fun saveDraft(
        @Valid @RequestBody dto: OpenRequestDto,
    ): ResponseEntity<DraftCreatedResponse> {
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
            DraftCreatedResponse(
                id = id,
                estado = "RASCUNHO",
                links = mapOf(
                    "self" to "/requests/$id",
                    "submit" to "/requests/$id/submit",
                    "update-draft" to "/requests/$id/draft",
                    "upload-url" to "/requests/$id/attachments/upload-url",
                ),
            ),
        )
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Submeter rascunho como solicitação oficial")
    fun submitDraft(
        @PathVariable id: UUID,
    ): ResponseEntity<DraftSubmittedResponse> {
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
            DraftSubmittedResponse(
                id = entity.id,
                estado = "ABERTA",
                protocolo = protocolo,
                links = mapOf("self" to "/requests/$id"),
            ),
        )
    }

    @PatchMapping("/{id}/draft")
    @PreAuthorize("hasAuthority('request.open')")
    @Operation(summary = "Atualizar dados de um rascunho (estado RASCUNHO)")
    fun updateDraft(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateDraftDto,
    ): ResponseEntity<DraftCreatedResponse> {
        val user = currentUser()
        val updatedId =
            updateDraftUseCase.execute(
                UpdateDraftCommand(
                    requestId = id,
                    idSolicitante = user.userId,
                    dados = dto.dados,
                ),
            )
        return ResponseEntity.ok(
            DraftCreatedResponse(
                id = updatedId,
                estado = "RASCUNHO",
                links = mapOf(
                    "self" to "/requests/$id",
                    "submit" to "/requests/$id/submit",
                    "update-draft" to "/requests/$id/draft",
                    "upload-url" to "/requests/$id/attachments/upload-url",
                ),
            ),
        )
    }

    @GetMapping("/{id}/protocol")
    @PreAuthorize("hasAuthority('request.view_own') or hasAuthority('request.view_curso')")
    @Operation(summary = "Informações de protocolo da solicitação")
    fun getProtocol(
        @PathVariable id: UUID,
    ): ResponseEntity<RequestProtocolResponse> =
        ResponseEntity.ok(requestQuery.getProtocol(id, currentUser()))

    @GetMapping
    @PreAuthorize("hasAnyAuthority('request.view_own', 'request.view_curso', 'request.deliberate')")
    @Operation(summary = "Listar solicitações com filtros")
    fun list(
        @RequestParam(required = false) estado: String?,
        @RequestParam(required = false) idCurso: UUID?,
        @RequestParam(required = false) typeCode: String?,
        @RequestParam(required = false) type: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<RequestSummaryResponse> =
        requestQuery.list(currentUser(), estado, idCurso, typeCode, type, pageable)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('request.view_own', 'request.view_curso', 'request.deliberate')")
    @Operation(summary = "Detalhe de uma solicitação com HATEOAS transitions e formSchema para render")
    fun getById(
        @PathVariable id: UUID,
    ): RequestDetailResponse = requestQuery.getById(id, currentUser())

    @PostMapping("/{id}/transitions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aplicar transição de workflow (DEFER, DENY, ASSIGN, etc.)")
    fun transition(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: TransitionDto,
    ): ResponseEntity<TransitionAppliedResponse> {
        val user = currentUser()
        val result = transitionUseCase.execute(
            TransitionCommand(
                requestId = id,
                action = dto.action,
                actorId = user.userId,
                actorAuthorities = user.authorities,
                parecer = dto.parecer,
            ),
        )
        val response = TransitionAppliedResponse(
            mensagem = "Transição '${dto.action}' aplicada com sucesso.",
            estadoNovo = result.newState.name,
            links = mapOf("self" to "/requests/$id"),
        )
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/bulk-deliberate")
    @PreAuthorize("hasAuthority('request.deliberate') or hasAuthority('image_authorization.review')")
    @Operation(summary = "Deliberar várias solicitações na mesma transação (all-or-nothing)")
    fun bulkDeliberate(
        @Valid @RequestBody dto: BulkDeliberateDto,
    ): ResponseEntity<BulkDeliberateResponse> {
        val user = currentUser()
        val processados =
            bulkDeliberateUseCase.execute(
                BulkDeliberateCommand(
                    ids = dto.ids,
                    action = dto.action,
                    actorId = user.userId,
                    actorAuthorities = user.authorities,
                    parecer = dto.parecer,
                ),
            )
        return ResponseEntity.ok(BulkDeliberateResponse(processados = processados, action = dto.action))
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyAuthority('request.view_own', 'request.view_curso', 'request.deliberate')")
    @Operation(summary = "Histórico de eventos (trilha de auditoria) de uma solicitação")
    fun events(
        @PathVariable id: UUID,
    ): List<RequestEventResponse> = requestQuery.events(id)

    /** API-01/02: List all active request types for the wizard. */
    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar tipos de solicitação ativos com formulário JSON Schema")
    fun listTypes(): List<RequestTypeSummaryResponse> = requestQuery.listTypes()

    /** API-02: Get single active type by code — used by wizard to load schema before submission. */
    @GetMapping("/types/{code}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalhe de tipo de solicitação por código (para wizard)")
    fun getTypeByCode(
        @PathVariable code: String,
    ): RequestTypeDetailResponse = requestQuery.getTypeByCode(code)

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
