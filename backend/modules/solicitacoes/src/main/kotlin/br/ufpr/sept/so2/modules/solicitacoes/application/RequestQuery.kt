package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestDetailResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestEventResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestProtocolResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestSummaryResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestTypeDetailResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestTypeSummaryResponse
import br.ufpr.sept.so2.modules.solicitacoes.domain.AttachmentPolicy
import br.ufpr.sept.so2.modules.solicitacoes.domain.RequestState
import br.ufpr.sept.so2.modules.solicitacoes.domain.WorkflowDefinition
import br.ufpr.sept.so2.modules.solicitacoes.domain.WorkflowEngine
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import br.ufpr.sept.so2.shared.security.AuthenticatedUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RequestQuery(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val requestEventRepo: RequestEventJpaRepository,
    private val versionStore: RequestTypeVersionStore,
    private val objectMapper: ObjectMapper,
) {
    fun list(
        user: AuthenticatedUser,
        estado: String?,
        idCurso: UUID?,
        typeCode: String?,
        type: String?,
        pageable: Pageable,
    ): PageResponse<RequestSummaryResponse> {
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
        return PageResponse.ofWithLinks(page) { r ->
            val links = mutableMapOf("self" to "/requests/${r.id}")
            if (canBulk && r.estado == "ABERTA") {
                links["bulk_deliberate"] = "/requests/bulk-deliberate"
            }
            RequestSummaryResponse(
                id = r.id,
                numeroAnual = r.numeroAnual,
                ano = r.ano.toInt(),
                tipoCode = r.requestTypeCode,
                estado = r.estado,
                prazoEm = r.prazoEm,
                idSolicitante = r.idSolicitante,
                protocolo = "${r.ano}/${r.numeroAnual.toString().padStart(4, '0')}",
                links = links,
            )
        }
    }

    fun getById(
        id: UUID,
        user: AuthenticatedUser,
    ): RequestDetailResponse {
        val entity =
            requestRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: $id") }

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

        val protocolo = "${entity.ano}/${entity.numeroAnual.toString().padStart(4, '0')}"
        val links = linkedMapOf(
            "self" to "/requests/$id",
            "events" to "/requests/$id/events",
            "attachments" to "/requests/$id/attachments",
        )
        if (entity.estado == "RASCUNHO" && entity.idSolicitante == user.userId) {
            links["submit"] = "/requests/$id/submit"
            links["update-draft"] = "/requests/$id/draft"
            links["upload-url"] = "/requests/$id/attachments/upload-url"
        } else if (entity.estado in AttachmentPolicy.MODIFIABLE_STATES &&
            entity.idSolicitante == user.userId
        ) {
            links["upload-url"] = "/requests/$id/attachments/upload-url"
        }
        allowedTransitions.forEach { transition ->
            links[transition.action.lowercase().replace('_', '-')] = "/requests/$id/transitions"
        }

        return RequestDetailResponse(
            id = entity.id,
            numeroAnual = entity.numeroAnual,
            ano = entity.ano.toInt(),
            protocolo = protocolo,
            tipoCode = entity.requestTypeCode,
            tipoDescricao = requestType.descricao,
            estado = entity.estado,
            dados = entity.dados,
            parecer = entity.parecer,
            prazoEm = entity.prazoEm,
            concludedAt = entity.concludedAt,
            createdAt = entity.createdAt,
            idSolicitante = entity.idSolicitante,
            formSchema = versionStore.formSchemaFor(entity, requestType.formSchema),
            idRequestTypeVersion = entity.idRequestTypeVersion,
            links = links,
        )
    }

    fun getProtocol(
        id: UUID,
        user: AuthenticatedUser,
    ): RequestProtocolResponse {
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
        return RequestProtocolResponse(
            protocolo = protocolo,
            tipo = entity.requestTypeCode,
            estado = entity.estado,
            idSolicitante = entity.idSolicitante,
            createdAt = entity.createdAt,
            links = mapOf(
                "self" to "/requests/$id",
                "public" to "/publico/solicitacoes/${entity.ano}/${entity.numeroAnual}",
            ),
        )
    }

    fun events(id: UUID): List<RequestEventResponse> =
        requestEventRepo.findAllByIdRequestOrderByCreatedAtAsc(id).map { e ->
            RequestEventResponse(
                tipo = e.tipo,
                estadoAnterior = e.estadoAnterior,
                estadoNovo = e.estadoNovo,
                parecer = e.parecer,
                createdAt = e.createdAt,
            )
        }

    fun listTypes(): List<RequestTypeSummaryResponse> =
        requestTypeRepo.findAllByAtivoTrue().map { rt ->
            RequestTypeSummaryResponse(
                id = rt.id,
                code = rt.code,
                descricao = rt.descricao,
                prazoDias = rt.prazoDias,
                formSchema = rt.formSchema,
                links = mapOf(
                    "self" to "/requests/types/${rt.code}",
                    "open" to "/requests",
                    "save-draft" to "/requests/draft",
                ),
            )
        }

    fun getTypeByCode(code: String): RequestTypeDetailResponse {
        val rt = requestTypeRepo.findByCode(code.uppercase())
            .orElseThrow { NoSuchElementException("Tipo de solicitação não encontrado: $code") }
        require(rt.ativo) { "Tipo de solicitação inativo: $code" }
        return RequestTypeDetailResponse(
            id = rt.id,
            code = rt.code,
            descricao = rt.descricao,
            formSchema = rt.formSchema,
            workflowJson = rt.workflowJson,
            prazoDias = rt.prazoDias,
            ativo = rt.ativo,
            links = mapOf(
                "self" to "/requests/types/${rt.code}",
                "open" to "/requests",
                "save-draft" to "/requests/draft",
            ),
        )
    }
}
