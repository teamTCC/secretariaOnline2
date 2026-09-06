package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipEntity
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

data class DeclararEstagioCommand(
    val idAluno: UUID,
    val empresa: String,
    val cargo: String,
    val cargaHorariaSemanal: Int,
    val inicio: LocalDate,
    val observacoes: String?,
)

data class EstagioCreatedResult(
    val id: UUID,
    val estado: String,
)

@Service
@Transactional
class DeclararEstagioUseCase(
    private val internshipRepo: InternshipJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun execute(command: DeclararEstagioCommand): EstagioCreatedResult {
        val entity =
            InternshipEntity(
                idAluno = command.idAluno,
                empresa = command.empresa,
                cargo = command.cargo,
                cargaHorariaSemanal = command.cargaHorariaSemanal,
                inicio = command.inicio,
                observacoes = command.observacoes,
            )
        val saved = internshipRepo.save(entity)
        outboxPublisher.enqueue(
            eventType = "estagio.declarado",
            aggregateType = "internship",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "internshipId" to saved.id.toString(),
                    "idAluno" to command.idAluno.toString(),
                    "empresa" to command.empresa,
                ),
        )
        return EstagioCreatedResult(id = saved.id, estado = saved.estado)
    }
}
