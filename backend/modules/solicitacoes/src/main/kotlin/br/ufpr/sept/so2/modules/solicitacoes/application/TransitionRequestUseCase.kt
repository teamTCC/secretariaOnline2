package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.domain.Request
import br.ufpr.sept.so2.modules.solicitacoes.domain.RequestState
import br.ufpr.sept.so2.modules.solicitacoes.domain.WorkflowDefinition
import br.ufpr.sept.so2.modules.solicitacoes.domain.WorkflowEngine
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class TransitionCommand(
    val requestId: UUID,
    val action: String,
    val actorId: UUID,
    val actorAuthorities: Set<String>,
    val parecer: String?,
)

@Service
class TransitionRequestUseCase(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val requestEventRepo: RequestEventJpaRepository,
    private val outboxRepo: OutboxEventJpaRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun execute(command: TransitionCommand) {
        val entity =
            requestRepo
                .findById(command.requestId)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: ${command.requestId}") }

        val requestType =
            requestTypeRepo
                .findById(entity.idRequestType)
                .orElseThrow { NoSuchElementException("Tipo de solicitação não encontrado") }

        val workflowDef = objectMapper.convertValue(requestType.workflowJson, WorkflowDefinition::class.java)
        val engine = WorkflowEngine(workflowDef)

        val domainRequest =
            Request(
                id = entity.id,
                numeroAnual = entity.numeroAnual,
                ano = entity.ano,
                idRequestType = entity.idRequestType,
                requestTypeCode = entity.requestTypeCode,
                idSolicitante = entity.idSolicitante,
                idCurso = entity.idCurso,
                estado = RequestState.valueOf(entity.estado),
                dados = entity.dados,
                parecer = entity.parecer,
                prazoEm = entity.prazoEm,
                concludedAt = entity.concludedAt,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )

        val result =
            engine.applyTransition(
                request = domainRequest,
                action = command.action,
                actorId = command.actorId,
                actorAuthorities = command.actorAuthorities,
                parecer = command.parecer,
            )

        val isFinal = result.newState.isFinal()
        requestRepo.updateEstado(
            id = command.requestId,
            estado = result.newState.name,
            parecer = command.parecer,
            concluded = isFinal,
        )

        requestEventRepo.save(
            RequestEventEntity(
                idRequest = command.requestId,
                tipo = command.action,
                estadoAnterior = domainRequest.estado.name,
                estadoNovo = result.newState.name,
                idAtor = command.actorId,
                parecer = command.parecer,
            ),
        )

        // Enqueue outbox event so the OutboxDispatcher can send email/push
        // notifications to the applicant asynchronously — same transaction ensures
        // at-least-once delivery without needing a message broker.
        outboxRepo.save(
            OutboxEventEntity(
                eventType = "solicitacoes.${command.action.lowercase()}",
                aggregateType = "Request",
                aggregateId = command.requestId,
                payload =
                    mapOf(
                        "requestId" to command.requestId.toString(),
                        "action" to command.action,
                        "estadoAnterior" to domainRequest.estado.name,
                        "estadoNovo" to result.newState.name,
                        "idSolicitante" to entity.idSolicitante.toString(),
                        "tipoCode" to entity.requestTypeCode,
                        "parecer" to (command.parecer ?: ""),
                    ),
            ),
        )
    }
}
