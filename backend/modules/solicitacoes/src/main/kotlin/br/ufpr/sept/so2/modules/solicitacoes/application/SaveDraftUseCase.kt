package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestAttachmentJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class SaveDraftCommand(
    val idRequestType: UUID,
    val idSolicitante: UUID,
    val idCurso: UUID,
    val dados: Map<String, Any>,
    val attachments: List<AttachmentInput> = emptyList(),
)

@Service
class SaveDraftUseCase(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
    private val attachmentRepo: RequestAttachmentJpaRepository,
) {
    @Transactional
    fun execute(command: SaveDraftCommand): UUID {
        val requestType =
            requestTypeRepo
                .findById(command.idRequestType)
                .orElseThrow { NoSuchElementException("Tipo de solicitação não encontrado: ${command.idRequestType}") }
        require(requestType.ativo) { "Tipo de solicitação inativo: ${requestType.code}" }

        val entity =
            RequestEntity(
                numeroAnual = 0,
                ano = OffsetDateTime.now().year.toShort(),
                idRequestType = requestType.id,
                requestTypeCode = requestType.code,
                idSolicitante = command.idSolicitante,
                idCurso = command.idCurso,
                estado = "RASCUNHO",
                dados = command.dados,
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

        return saved.id
    }
}
