package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.estagio.domain.EstagioNotFoundException
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class AssignSupervisorCommand(
    val internshipId: UUID,
    val idSupervisor: UUID,
)

data class AssignSupervisorResult(
    val id: UUID,
    val idSupervisor: UUID?,
)

data class BulkAssignSupervisorCommand(
    val internshipIds: List<UUID>,
    val idSupervisor: UUID,
)

@Service
@Transactional
class AssignSupervisorUseCase(
    private val internshipRepo: InternshipJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun assignSupervisor(command: AssignSupervisorCommand): AssignSupervisorResult {
        val internship =
            internshipRepo.findById(command.internshipId).orElseThrow { EstagioNotFoundException(command.internshipId) }
        internship.idSupervisor = command.idSupervisor
        internshipRepo.save(internship)
        outboxPublisher.enqueue(
            eventType = "estagio.supervisor_atribuido",
            aggregateType = "Internship",
            aggregateId = command.internshipId,
            payload =
                mapOf(
                    "internshipId" to command.internshipId.toString(),
                    "idSupervisor" to command.idSupervisor.toString(),
                    "idAluno" to internship.idAluno.toString(),
                ),
        )
        return AssignSupervisorResult(id = internship.id, idSupervisor = internship.idSupervisor)
    }

    fun bulkAssign(command: BulkAssignSupervisorCommand): Int {
        val internships = internshipRepo.findAllById(command.internshipIds)
        internships.forEach { i -> i.idSupervisor = command.idSupervisor }
        internshipRepo.saveAll(internships)
        internships.forEach { i ->
            outboxPublisher.enqueue(
                eventType = "estagio.supervisor_atribuido",
                aggregateType = "Internship",
                aggregateId = i.id,
                payload =
                    mapOf(
                        "internshipId" to i.id.toString(),
                        "idSupervisor" to command.idSupervisor.toString(),
                        "idAluno" to i.idAluno.toString(),
                    ),
            )
        }
        return internships.size
    }
}
