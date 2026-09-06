package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.HistoricoEscolarEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.HistoricoEscolarJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class HistoricoUpsertResult(
    val id: UUID,
    val estado: String,
)

data class UpsertHistoricoCommand(
    val alunoId: UUID,
    val disciplinaId: UUID,
    val estado: String,
)

@Service
@Transactional
class UpsertHistoricoEscolarUseCase(
    private val historicoRepo: HistoricoEscolarJpaRepository,
    private val disciplinaRepo: DisciplinaJpaRepository,
) {
    fun execute(command: UpsertHistoricoCommand): HistoricoUpsertResult {
        val estado = command.estado.uppercase()
        require(estado in setOf("CURSANDO", "CONCLUIDA", "REPROVADA")) {
            "estado deve ser CURSANDO, CONCLUIDA ou REPROVADA."
        }
        disciplinaRepo.findById(command.disciplinaId)
            .orElseThrow { NoSuchElementException("Disciplina não encontrada: ${command.disciplinaId}") }

        val entity =
            historicoRepo.findByIdAlunoAndIdDisciplina(command.alunoId, command.disciplinaId).orElse(
                HistoricoEscolarEntity(
                    idAluno = command.alunoId,
                    idDisciplina = command.disciplinaId,
                    estado = estado,
                ),
            )
        entity.estado = estado
        val saved = historicoRepo.save(entity)
        return HistoricoUpsertResult(id = saved.id, estado = saved.estado)
    }
}
