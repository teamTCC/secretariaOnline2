package br.ufpr.sept.so2.modules.formativas.application

import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityEntity
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

data class SubmitFormativaCommand(
    val idAluno: UUID,
    val titulo: String,
    val descricao: String?,
    val categoria: String,
    val cargaHoraria: Double,
    val dataRealizacao: LocalDate,
    val storageKeyComprovante: String?,
)

data class SubmitFormativaResult(
    val id: UUID,
    val estado: String,
)

@Service
@Transactional
class SubmitFormativaUseCase(
    private val activityRepo: FormativeActivityJpaRepository,
) {
    fun execute(command: SubmitFormativaCommand): SubmitFormativaResult {
        val entity =
            FormativeActivityEntity(
                idAluno = command.idAluno,
                titulo = command.titulo,
                descricao = command.descricao,
                categoria = command.categoria,
                cargaHoraria = command.cargaHoraria,
                dataRealizacao = command.dataRealizacao,
                storageKeyComprovante = command.storageKeyComprovante,
            )
        val saved = activityRepo.save(entity)
        return SubmitFormativaResult(id = saved.id, estado = saved.estado)
    }
}
