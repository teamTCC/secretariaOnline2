package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.tcc.domain.TccBusinessException
import br.ufpr.sept.so2.modules.tcc.domain.TccNotFoundException
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

data class UpdateTccCommand(
    val id: UUID,
    val titulo: String?,
    val dataDefesa: LocalDate?,
    val idOrientador: UUID,
)

@Service
@Transactional
class UpdateTccUseCase(
    private val tccRepo: TccJpaRepository,
) {
    fun execute(command: UpdateTccCommand): UUID {
        val tcc = tccRepo.findById(command.id).orElseThrow { TccNotFoundException(command.id) }
        if (tcc.idOrientador != command.idOrientador) {
            throw TccBusinessException("Você não é o orientador deste TCC.")
        }
        command.titulo?.let { tcc.titulo = it }
        command.dataDefesa?.let { tcc.dataDefesa = it }
        return tccRepo.save(tcc).id
    }
}
