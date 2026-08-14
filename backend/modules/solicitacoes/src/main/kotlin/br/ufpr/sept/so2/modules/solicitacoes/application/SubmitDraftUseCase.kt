package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class SubmitDraftCommand(
    val requestId: UUID,
    val idSolicitante: UUID,
)

@Service
class SubmitDraftUseCase(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val outboxRepo: OutboxEventJpaRepository,
) {
    @Transactional
    fun execute(command: SubmitDraftCommand): RequestEntity {
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

        val ultimoNumero = requestRepo.findMaxNumeroAnual(entity.ano, entity.idCurso) ?: 0
        entity.numeroAnual = ultimoNumero + 1
        entity.estado = "ABERTA"
        entity.prazoEm = OffsetDateTime.now().plusDays(requestType.prazoDias.toLong())

        val saved = requestRepo.save(entity)

        outboxRepo.save(
            OutboxEventEntity(
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
            ),
        )

        return saved
    }
}
