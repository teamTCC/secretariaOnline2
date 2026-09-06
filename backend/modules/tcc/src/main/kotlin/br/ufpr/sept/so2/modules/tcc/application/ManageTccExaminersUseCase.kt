package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.tcc.domain.TccNotFoundException
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccExaminerEntity
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccExaminerJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class AddTccExaminerCommand(
    val idTcc: UUID,
    val idProfessor: UUID,
    val papel: String,
)

data class TccExaminerResult(
    val idTcc: UUID,
    val idProfessor: UUID,
    val papel: String,
)

@Service
@Transactional
class ManageTccExaminersUseCase(
    private val tccRepo: TccJpaRepository,
    private val examinerRepo: TccExaminerJpaRepository,
) {
    fun addExaminer(command: AddTccExaminerCommand): TccExaminerResult {
        tccRepo.findById(command.idTcc).orElseThrow { TccNotFoundException(command.idTcc) }
        val examiner =
            TccExaminerEntity(idTcc = command.idTcc, idProfessor = command.idProfessor, papel = command.papel)
        examinerRepo.save(examiner)
        return TccExaminerResult(idTcc = command.idTcc, idProfessor = command.idProfessor, papel = command.papel)
    }
}
