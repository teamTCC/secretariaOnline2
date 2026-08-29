package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.tcc.domain.TccBusinessException
import br.ufpr.sept.so2.modules.tcc.domain.TccNotFoundException
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ApproveTccCommand(
    val idTcc: UUID,
    val aprovado: Boolean,
    val notaFinal: Double?,
    val idOrientador: UUID,
)

data class ApproveTccResult(
    val estado: String,
    val aprovado: Boolean?,
    val notaFinal: Double?,
)

@Service
@Transactional
class ApproveTccUseCase(
    private val tccRepo: TccJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun execute(command: ApproveTccCommand): ApproveTccResult {
        val tcc = tccRepo.findById(command.idTcc).orElseThrow { TccNotFoundException(command.idTcc) }
        if (tcc.idOrientador != command.idOrientador) {
            throw TccBusinessException("Você não é o orientador deste TCC.")
        }
        tcc.aprovado = command.aprovado
        tcc.notaFinal = command.notaFinal
        tcc.estado = if (command.aprovado) "APROVADO" else "REPROVADO"
        tccRepo.save(tcc)
        outboxPublisher.enqueue(
            eventType = "tcc.deliberado",
            aggregateType = "tcc",
            aggregateId = tcc.id,
            payload =
                mapOf(
                    "tccId" to tcc.id.toString(),
                    "aprovado" to command.aprovado,
                    "notaFinal" to (command.notaFinal ?: ""),
                ),
        )
        return ApproveTccResult(estado = tcc.estado, aprovado = tcc.aprovado, notaFinal = tcc.notaFinal)
    }
}
