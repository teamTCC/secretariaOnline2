package br.ufpr.sept.so2.modules.tcc.application

import br.ufpr.sept.so2.modules.tcc.domain.TccBusinessException
import br.ufpr.sept.so2.modules.tcc.domain.TccNotFoundException
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccJpaRepository
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccMemberEntity
import br.ufpr.sept.so2.modules.tcc.infrastructure.persistence.TccMemberJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class AddTccMemberCommand(
    val idTcc: UUID,
    val idAluno: UUID,
    val papel: String,
    val idOrientador: UUID,
)

data class TccMemberResult(
    val idTcc: UUID,
    val idAluno: UUID,
    val papel: String,
)

@Service
@Transactional
class ManageTccMembersUseCase(
    private val tccRepo: TccJpaRepository,
    private val memberRepo: TccMemberJpaRepository,
) {
    fun addMember(command: AddTccMemberCommand): TccMemberResult {
        val tcc = tccRepo.findById(command.idTcc).orElseThrow { TccNotFoundException(command.idTcc) }
        if (tcc.idOrientador != command.idOrientador) {
            throw TccBusinessException("Você não é o orientador deste TCC.")
        }
        val member = TccMemberEntity(idTcc = command.idTcc, idAluno = command.idAluno, papel = command.papel)
        memberRepo.save(member)
        return TccMemberResult(idTcc = command.idTcc, idAluno = command.idAluno, papel = command.papel)
    }
}
