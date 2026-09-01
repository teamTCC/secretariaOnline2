package sistemaonline.example.demo.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import sistemaonline.example.demo.model.Curso
import sistemaonline.example.demo.repository.CursoRepository

@Service
class CursoService(private val repository: CursoRepository) {

    fun findAll(): List<Curso> = repository.findAll()

    fun findById(id: Long): Curso? = repository.findByIdOrNull(id)

    fun save(curso: Curso): Curso = repository.save(curso)

    fun update(id: Long, cursoAtualizado: Curso): Curso? {
        val cursoExistente = repository.findByIdOrNull(id) ?: return null
        
        val cursoParaSalvar = cursoExistente.copy(
            nome = cursoAtualizado.nome,
            sigla = cursoAtualizado.sigla,
            codigo = cursoAtualizado.codigo,
            horasFormativasReq = cursoAtualizado.horasFormativasReq,
            ativo = cursoAtualizado.ativo
        )
        
        return repository.save(cursoParaSalvar)
    }

    fun delete(id: Long) {
        if (repository.existsById(id)) {
            repository.deleteById(id)
        }
    }
}
