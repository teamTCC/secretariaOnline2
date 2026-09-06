package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccEntity
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class CreateTccCommand(
    val titulo: String,
    val idCurso: UUID,
    val idOrientador: UUID,
)

data class TccCreatedResult(
    val id: UUID,
    val estado: String,
)

@Service
@Transactional
class CreateTccUseCase(
    private val tccRepo: TccJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun execute(command: CreateTccCommand): TccCreatedResult {
        val tcc =
            TccEntity(
                idOrientador = command.idOrientador,
                titulo = command.titulo,
                idCurso = command.idCurso,
                estado = "EM_ANDAMENTO",
            )
        val saved = tccRepo.save(tcc)
        outboxPublisher.enqueue(
            eventType = "tcc.criado",
            aggregateType = "tcc",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "tccId" to saved.id.toString(),
                    "idOrientador" to command.idOrientador.toString(),
                    "titulo" to command.titulo,
                ),
        )
        return TccCreatedResult(id = saved.id, estado = saved.estado)
    }
}
