package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventEntity
import br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence.OutboxEventJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
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
)

@Service
class OpenRequestUseCase(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val attachmentRepo: RequestAttachmentJpaRepository,
    private val outboxRepo: OutboxEventJpaRepository,
) {
    @Transactional
    fun execute(command: OpenRequestCommand): UUID {
        val requestType =
            requestTypeRepo
                .findById(command.idRequestType)
                .orElseThrow { NoSuchElementException("Tipo de solicitação não encontrado: ${command.idRequestType}") }

        require(requestType.ativo) { "Tipo de solicitação inativo: ${requestType.code}" }

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
                idSolicitante = command.idSolicitante,
                idCurso = command.idCurso,
                estado = "ABERTA",
                dados = command.dados,
                prazoEm = prazoEm,
            )
        val saved = requestRepo.save(entity)

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
                        "idCurso" to command.idCurso.toString(),
                        "estadoNovo" to "ABERTA",
                    ),
            ),
        )

        return saved.id
    }
}
