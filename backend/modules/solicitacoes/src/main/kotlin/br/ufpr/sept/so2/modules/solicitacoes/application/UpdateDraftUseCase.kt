package br.ufpr.sept.so2.modules.solicitacoes.application

import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UpdateDraftCommand(
    val requestId: UUID,
    val idSolicitante: UUID,
    val dados: Map<String, Any>,
)

@Service
class UpdateDraftUseCase(
    private val requestRepo: RequestJpaRepository,
) {
    @Transactional
    fun execute(command: UpdateDraftCommand): UUID {
        val entity =
            requestRepo
                .findById(command.requestId)
                .orElseThrow { NoSuchElementException("Solicitação não encontrada: ${command.requestId}") }
        require(entity.estado == "RASCUNHO") {
            "Só é possível atualizar rascunhos (estado atual: ${entity.estado})."
        }
        if (entity.idSolicitante != command.idSolicitante) {
            throw AccessDeniedException("Acesso negado: você não é o dono desta solicitação.")
        }
        entity.dados = command.dados
        return requestRepo.save(entity).id
    }
}
