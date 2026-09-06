package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.estagio.domain.EstagioNotFoundException
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

data class ConcludeEstagioCommand(
    val id: UUID,
)

data class EstagioEncerradoResult(
    val estado: String,
    val fim: LocalDate?,
)

@Service
@Transactional
class EncerrarEstagioUseCase(
    private val internshipRepo: InternshipJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun conclude(command: ConcludeEstagioCommand): EstagioEncerradoResult {
        val internship = internshipRepo.findById(command.id).orElseThrow { EstagioNotFoundException(command.id) }
        require(internship.estado == "EM_ANDAMENTO") { "Estágio não está EM_ANDAMENTO." }
        internship.estado = "CONCLUIDO"
        if (internship.fim == null) internship.fim = LocalDate.now()
        internshipRepo.save(internship)
        outboxPublisher.enqueue(
            eventType = "estagio.concluido",
            aggregateType = "internship",
            aggregateId = internship.id,
            payload =
                mapOf(
                    "internshipId" to internship.id.toString(),
                    "idAluno" to internship.idAluno.toString(),
                    "empresa" to internship.empresa,
                ),
        )
        return EstagioEncerradoResult(estado = internship.estado, fim = internship.fim)
    }
}
