package sistemaonline.example.demo.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import sistemaonline.example.demo.model.Aluno
import sistemaonline.example.demo.repository.AlunoRepository

@Service
class AlunoService(private val repository: AlunoRepository) {

    fun findAll(): List<Aluno> = repository.findAll()

    fun findById(id: Long): Aluno? = repository.findByIdOrNull(id)

    fun save(aluno: Aluno): Aluno = repository.save(aluno)

    fun update(id: Long, alunoAtualizado: Aluno): Aluno? {
        val alunoExistente = repository.findByIdOrNull(id) ?: return null
        
        val alunoParaSalvar = alunoExistente.copy(
            nome = alunoAtualizado.nome,
            idade = alunoAtualizado.idade
        )
        
        return repository.save(alunoParaSalvar)
    }

    fun delete(id: Long) {
        if (repository.existsById(id)) {
            repository.deleteById(id)
        }
    }
}
