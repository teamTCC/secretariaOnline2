package sistemaonline.example.demo.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import sistemaonline.example.demo.model.Disciplina
import sistemaonline.example.demo.repository.DisciplinaRepository

@Service
class DisciplinaService(private val repository: DisciplinaRepository) {

    fun findAll(): List<Disciplina> = repository.findAll()

    fun findById(id: Long): Disciplina? = repository.findByIdOrNull(id)

    fun save(disciplina: Disciplina): Disciplina = repository.save(disciplina)

    fun update(id: Long, disciplinaAtualizada: Disciplina): Disciplina? {
        val disciplinaExistente = repository.findByIdOrNull(id) ?: return null
        
        val disciplinaParaSalvar = disciplinaExistente.copy(
            codigo = disciplinaAtualizada.codigo,
            nome = disciplinaAtualizada.nome,
            periodo = disciplinaAtualizada.periodo,
            cargaHoraria = disciplinaAtualizada.cargaHoraria,
            ativa = disciplinaAtualizada.ativa
        )
        
        return repository.save(disciplinaParaSalvar)
    }

    fun delete(id: Long) {
        if (repository.existsById(id)) {
            repository.deleteById(id)
        }
    }
}
