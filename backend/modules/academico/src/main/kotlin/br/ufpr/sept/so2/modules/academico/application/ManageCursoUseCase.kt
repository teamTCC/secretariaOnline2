package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UpdateCursoCommand(
    val cursoId: UUID,
    val nome: String?,
    val sigla: String?,
)

data class CreateDisciplinaCommand(
    val idCurso: UUID,
    val codigo: String,
    val nome: String,
    val cargaHorariaTotal: Int,
    val creditos: Int,
)

data class CursoUpdatedResult(
    val id: UUID,
    val nome: String,
    val sigla: String,
)

data class DisciplinaCreatedResult(
    val id: UUID,
    val codigo: String,
    val nome: String,
)

@Service
@Transactional
class ManageCursoUseCase(
    private val cursoRepo: CursoJpaRepository,
    private val disciplinaRepo: DisciplinaJpaRepository,
) {
    fun updateCurso(command: UpdateCursoCommand): CursoUpdatedResult {
        val curso = cursoRepo.findById(command.cursoId)
            .orElseThrow { NoSuchElementException("Curso não encontrado: ${command.cursoId}") }
        command.nome?.let { curso.nome = it }
        command.sigla?.let { curso.sigla = it }
        val saved = cursoRepo.save(curso)
        return CursoUpdatedResult(id = saved.id, nome = saved.nome, sigla = saved.sigla)
    }

    fun createDisciplina(command: CreateDisciplinaCommand): DisciplinaCreatedResult {
        cursoRepo.findById(command.idCurso)
            .orElseThrow { NoSuchElementException("Curso não encontrado: ${command.idCurso}") }
        val saved =
            disciplinaRepo.save(
                DisciplinaEntity(
                    idCurso = command.idCurso,
                    codigo = command.codigo,
                    nome = command.nome,
                    cargaHorariaTotal = command.cargaHorariaTotal,
                    creditos = command.creditos,
                ),
            )
        return DisciplinaCreatedResult(id = saved.id, codigo = saved.codigo, nome = saved.nome)
    }
}
