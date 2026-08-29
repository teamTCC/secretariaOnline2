package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.domain.FormSchemaValidator
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import br.ufpr.sept.so2.shared.security.currentUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ManageRequestTypeUseCase(
    private val typeRepo: RequestTypeJpaRepository,
    private val requestRepo: RequestJpaRepository,
    private val versionStore: RequestTypeVersionStore,
    private val auditPublisher: AuditPublisher,
    private val objectMapper: ObjectMapper,
) {
    fun create(
        code: String,
        descricao: String,
        formSchema: Map<String, Any>,
        workflowJson: Map<String, Any>,
        prazoDias: Int,
    ): UUID {
        val normalized = code.uppercase()
        require(typeRepo.findByCode(normalized).isEmpty) { "Tipo já existe: $normalized" }
        require(prazoDias > 0) { "prazoDias deve ser positivo." }
        return typeRepo.save(
            RequestTypeEntity(
                code = normalized,
                descricao = descricao,
                formSchema = formSchema,
                workflowJson = workflowJson,
                prazoDias = prazoDias,
                ativo = false,
            ),
        ).id
    }

    fun delete(id: UUID) {
        typeRepo.findById(id).orElseThrow { NoSuchElementException("Tipo não encontrado: $id") }
        val used = requestRepo.countByIdRequestType(id)
        require(used == 0L) { "Não é possível excluir tipo com $used solicitações no histórico." }
        typeRepo.deleteById(id)
    }

    /** APP-06: Update draft (ativo=false) request type fields. Emits audit. */
    fun update(
        id: UUID,
        descricao: String,
        formSchema: Map<String, Any>,
        workflowJson: Map<String, Any>,
        prazoDias: Int,
    ): UUID {
        val entity = typeRepo.findById(id).orElseThrow { NoSuchElementException("Tipo não encontrado: $id") }
        require(prazoDias > 0) { "prazoDias deve ser positivo." }
        entity.descricao = descricao
        entity.formSchema = formSchema
        entity.workflowJson = workflowJson
        entity.prazoDias = prazoDias
        val saved = typeRepo.save(entity)

        // APP-10: Audit log every edit
        auditPublisher.publish(
            AuditPayload(
                acao = "request_type.update",
                idAtor = runCatching { currentUser().userId }.getOrNull(),
                alvoTipo = "RequestType",
                alvoId = id,
                ip = null,
                userAgent = null,
                resultado = "OK",
                detalhes = mapOf("code" to saved.code, "ativo" to saved.ativo),
            ),
        )

        return saved.id
    }

    /**
     * APP-07/08: Publish — validates form_schema + workflow_json structure before going live.
     * Once published (ativo=true), the type appears in GET /requests/types for students.
     */
    fun publish(id: UUID): UUID {
        val entity = typeRepo.findById(id).orElseThrow { NoSuchElementException("Tipo não encontrado: $id") }

        // APP-07: Validate form_schema structure
        FormSchemaValidator.validateSchemaStructure(entity.formSchema)

        // APP-07: Validate workflow_json structure (deserialization + state consistency)
        FormSchemaValidator.validateWorkflowStructure(entity.workflowJson, objectMapper)

        entity.ativo = true
        val saved = typeRepo.save(entity)
        versionStore.snapshot(saved)

        // APP-10: Audit log every publish
        auditPublisher.publish(
            AuditPayload(
                acao = "request_type.publish",
                idAtor = runCatching { currentUser().userId }.getOrNull(),
                alvoTipo = "RequestType",
                alvoId = id,
                ip = null,
                userAgent = null,
                resultado = "PUBLISHED",
                detalhes = mapOf("code" to saved.code),
            ),
        )

        return saved.id
    }
}
