package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class OpenRequestCommand(
    val idRequestType: UUID,
    val idSolicitante: UUID,
    val idCurso: UUID,
    val dados: Map<String, Any>,
)

@Service
class OpenRequestUseCase(
    private val requestRepo: RequestJpaRepository,
    private val requestTypeRepo: RequestTypeJpaRepository,
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
        return requestRepo.save(entity).id
    }
}
