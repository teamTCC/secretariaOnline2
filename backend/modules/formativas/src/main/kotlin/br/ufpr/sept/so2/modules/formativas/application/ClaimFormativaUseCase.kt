package br.ufpr.sept.so2.modules.formativas.application

import br.ufpr.sept.so2.modules.formativas.domain.FormativaBusinessException
import br.ufpr.sept.so2.modules.formativas.domain.FormativaNotFoundException
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ClaimFormativaCommand(
    val activityId: UUID,
    val revisorId: UUID,
)

data class ClaimFormativaResult(
    val id: UUID,
    val idRevisor: UUID,
)

@Service
@Transactional
class ClaimFormativaUseCase(
    private val activityRepo: FormativeActivityJpaRepository,
) {
    fun execute(command: ClaimFormativaCommand): ClaimFormativaResult {
        val activity =
            activityRepo
                .findById(command.activityId)
                .orElseThrow { FormativaNotFoundException(command.activityId) }

        if (activity.estado != "PENDENTE") {
            throw FormativaBusinessException("Atividade não está pendente: ${activity.estado}")
        }
        if (activity.idRevisor != null) {
            throw FormativaBusinessException("Atividade já foi reivindicada por outro membro.")
        }

        activity.idRevisor = command.revisorId
        activityRepo.save(activity)

        return ClaimFormativaResult(id = activity.id, idRevisor = command.revisorId)
    }
}
