package br.ufpr.sept.so2.modules.estagio.application

import br.ufpr.sept.so2.modules.estagio.domain.EstagioNotFoundException
import br.ufpr.sept.so2.modules.estagio.infrastructure.persistence.InternshipJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

data class AtualizarEstagioCommand(
    val id: UUID,
    val cargo: String?,
    val cargaHorariaSemanal: Int?,
    val fim: LocalDate?,
    val observacoes: String?,
    val idSupervisor: UUID?,
)

@Service
@Transactional
class AtualizarEstagioUseCase(
    private val internshipRepo: InternshipJpaRepository,
) {
    fun execute(command: AtualizarEstagioCommand): UUID {
        val internship = internshipRepo.findById(command.id).orElseThrow { EstagioNotFoundException(command.id) }
        command.cargo?.let { internship.cargo = it }
        command.cargaHorariaSemanal?.let { internship.cargaHorariaSemanal = it }
        command.fim?.let { internship.fim = it }
        command.observacoes?.let { internship.observacoes = it }
        command.idSupervisor?.let { internship.idSupervisor = it }
        return internshipRepo.save(internship).id
    }
}
