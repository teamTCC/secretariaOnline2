package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

data class CreatePeriodoCommand(
    val ano: Short,
    val semestre: Short,
    val inicio: LocalDate,
    val fim: LocalDate,
)

data class PeriodoCreatedResult(
    val id: UUID,
    val ano: Short,
    val semestre: Short,
)

data class ActivatePeriodoCommand(
    val periodoId: UUID,
)

@Service
@Transactional
class ManagePeriodoLetivoUseCase(
    private val periodoRepo: PeriodoLetivoJpaRepository,
) {
    fun create(command: CreatePeriodoCommand): PeriodoCreatedResult {
        require(command.fim.isAfter(command.inicio)) { "Data fim deve ser após data início." }
        val saved =
            periodoRepo.save(
                PeriodoLetivoEntity(
                    ano = command.ano,
                    semestre = command.semestre,
                    inicio = command.inicio,
                    fim = command.fim,
                ),
            )
        return PeriodoCreatedResult(id = saved.id, ano = saved.ano, semestre = saved.semestre)
    }

    fun activate(command: ActivatePeriodoCommand): UUID {
        periodoRepo.findAll().filter { it.ativo }.forEach {
            it.ativo = false
            periodoRepo.save(it)
        }
        val periodo = periodoRepo.findById(command.periodoId)
            .orElseThrow { NoSuchElementException("Período letivo não encontrado: ${command.periodoId}") }
        periodo.ativo = true
        return periodoRepo.save(periodo).id
    }
}
