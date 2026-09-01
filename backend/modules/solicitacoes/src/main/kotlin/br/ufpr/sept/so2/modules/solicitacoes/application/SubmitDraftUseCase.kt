package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.domain.AttachmentPolicy
import br.ufpr.sept.so2.modules.solicitacoes.domain.FormSchemaValidator
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class SubmitDraftCommand(
    val requestId: UUID,
    val idSolicitante: UUID,
)

data class SubmitDraftResult(
    val id: UUID,
    val ano: Short,
    val numeroAnual: Int,
)

@Service
class SubmitDraftUseCase(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val attachmentRepo: RequestAttachmentJpaRepository,
    private val requestEventRepo: RequestEventJpaRepository,
) {
    @Transactional
    fun execute(command: SubmitDraftCommand): SubmitDraftResult {
        val entity =
            requestRepo.findById(command.requestId)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: ${command.requestId}") }
        require(entity.estado == "RASCUNHO") {
            "Solicitação não está em estado RASCUNHO (estado atual: ${entity.estado})"
        }
        require(entity.idSolicitante == command.idSolicitante) {
            "Acesso negado: você não é o dono desta solicitação."
        }

        val requestType = requestTypeRepo.findById(entity.idRequestType).orElseThrow()

        // APP-02: Re-validate dados against form_schema when submitting a draft
        FormSchemaValidator.validate(entity.dados, requestType.formSchema)
        AttachmentPolicy.assertRequiredAttachments(
            requestType.formSchema,
            attachmentRepo.findAllByIdRequest(entity.id).map { it.categoria },
        )

        val ultimoNumero = requestRepo.findMaxNumeroAnual(entity.ano, entity.idCurso) ?: 0
        entity.numeroAnual = ultimoNumero + 1
        entity.estado = "ABERTA"
        entity.prazoEm = OffsetDateTime.now().plusDays(requestType.prazoDias.toLong())

        val saved = requestRepo.save(entity)

        requestEventRepo.save(
            RequestEventEntity(
                idRequest = saved.id,
                tipo = "ABERTURA",
                estadoAnterior = "RASCUNHO",
                estadoNovo = "ABERTA",
                idAtor = command.idSolicitante,
            ),
        )

        outboxPublisher.enqueue(
            eventType = "solicitacoes.aberta",
            aggregateType = "Request",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "requestId" to saved.id.toString(),
                    "tipoCode" to requestType.code,
                    "idSolicitante" to command.idSolicitante.toString(),
                    "idCurso" to entity.idCurso.toString(),
                    "estadoNovo" to "ABERTA",
                ),
        )

        return SubmitDraftResult(id = saved.id, ano = saved.ano, numeroAnual = saved.numeroAnual)
    }
}
