package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.tcc.domain.TccNotFoundException
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccExaminerJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class GradeDefenseCommand(
    val idTcc: UUID,
    val idProfessor: UUID,
    val nota: Double,
)

data class GradeResult(
    val idProfessor: UUID,
    val nota: Double,
)

@Service
@Transactional
class GradeDefenseUseCase(
    private val tccRepo: TccJpaRepository,
    private val examinerRepo: TccExaminerJpaRepository,
) {
    fun execute(command: GradeDefenseCommand): GradeResult {
        tccRepo.findById(command.idTcc).orElseThrow { TccNotFoundException(command.idTcc) }
        val examiner =
            examinerRepo.findAllByIdTcc(command.idTcc).find { it.idProfessor == command.idProfessor }
                ?: throw AccessDeniedException("Você não é membro da banca deste TCC.")
        examiner.nota = command.nota
        examinerRepo.save(examiner)
        return GradeResult(idProfessor = command.idProfessor, nota = command.nota)
    }
}
