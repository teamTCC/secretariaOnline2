package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.domain.AttachmentPolicy
import br.ufpr.sept.so2.modules.solicitacoes.domain.FormSchemaValidator
import br.ufpr.sept.so2.modules.solicitacoes.domain.WorkflowDefinition
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class AttachmentInput(
    val storageKey: String,
    val sha256: String,
    val nomeOriginal: String,
    val contentType: String,
    val categoria: String,
    val tamanhoBytes: Long,
)

data class OpenRequestCommand(
    val idRequestType: UUID,
    val idSolicitante: UUID,
    val idCurso: UUID,
    val dados: Map<String, Any>,
    val attachments: List<AttachmentInput> = emptyList(),
    /** When non-null, a secretaria is opening the request on behalf of the student. */
    val onBehalfOfId: UUID? = null,
)

@Service
class OpenRequestUseCase(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val attachmentRepo: RequestAttachmentJpaRepository,
    private val requestEventRepo: RequestEventJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val versionStore: RequestTypeVersionStore,
    private val objectMapper: ObjectMapper,
    private val storageGuard: AttachmentStorageGuard,
) {
    @Transactional
    fun execute(command: OpenRequestCommand): UUID {
        val requestType =
            requestTypeRepo
                .findById(command.idRequestType)
                .orElseThrow { NoSuchElementException("Tipo de solicitação não encontrado: ${command.idRequestType}") }

        require(requestType.ativo) { "Tipo de solicitação inativo: ${requestType.code}" }

        // APP-02: Validate dados against form_schema before persisting
        FormSchemaValidator.validate(command.dados, requestType.formSchema)
        AttachmentPolicy.assertRequiredAttachments(
            requestType.formSchema,
            command.attachments.map { it.categoria },
        )
        command.attachments.forEach { att ->
            AttachmentPolicy.assertUploadMetadata(att.contentType, att.tamanhoBytes)
            storageGuard.assertObjectMatches(att.storageKey, att.sha256, att.tamanhoBytes, requestId = null)
        }

        // DOM-05: Read initial state from workflow_json.initial — never hardcode "ABERTA"
        val workflowDef = objectMapper.convertValue(requestType.workflowJson, WorkflowDefinition::class.java)
        val initialState = workflowDef.initial

        val effectiveSolicitanteId = command.onBehalfOfId ?: command.idSolicitante

        val ano = OffsetDateTime.now().year.toShort()
        val ultimoNumero = requestRepo.findMaxNumeroAnual(ano, command.idCurso) ?: 0
        val numeroAnual = ultimoNumero + 1
        val prazoEm = OffsetDateTime.now().plusDays(requestType.prazoDias.toLong())

        val entity =
            RequestEntity(
                numeroAnual = numeroAnual,
                ano = ano,
                idRequestType = requestType.id,
                requestTypeCode = requestType.code,
                idSolicitante = effectiveSolicitanteId,
                idCurso = command.idCurso,
                estado = initialState,
                dados = command.dados,
                prazoEm = prazoEm,
                idRequestTypeVersion = versionStore.latestId(requestType.id),
            )
        val saved = requestRepo.save(entity)

        requestEventRepo.save(
            RequestEventEntity(
                idRequest = saved.id,
                tipo = "ABERTURA",
                estadoAnterior = "-",
                estadoNovo = initialState,
                idAtor = command.idSolicitante,
            ),
        )

        command.attachments.forEach { att ->
            attachmentRepo.save(
                RequestAttachmentEntity(
                    idRequest = saved.id,
                    categoria = att.categoria,
                    storageKey = att.storageKey,
                    sha256 = att.sha256,
                    nomeOriginal = att.nomeOriginal,
                    contentType = att.contentType,
                    tamanhoBytes = att.tamanhoBytes,
                ),
            )
        }

        val outboxPayload = mutableMapOf<String, Any>(
            "requestId" to saved.id.toString(),
            "tipoCode" to requestType.code,
            "idSolicitante" to effectiveSolicitanteId.toString(),
            "idCurso" to command.idCurso.toString(),
            "estadoNovo" to initialState,
        )
        if (command.onBehalfOfId != null) {
            outboxPayload["abertoPor"] = command.idSolicitante.toString()
        }

        outboxPublisher.enqueue(
            eventType = "solicitacoes.aberta",
            aggregateType = "Request",
            aggregateId = saved.id,
            payload = outboxPayload,
        )

        return saved.id
    }
}
