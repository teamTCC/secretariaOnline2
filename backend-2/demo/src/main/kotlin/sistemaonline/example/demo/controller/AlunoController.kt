package sistemaonline.example.demo.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import sistemaonline.example.demo.model.Aluno
import sistemaonline.example.demo.service.AlunoService

import io.swagger.v3.oas.annotations.tags.Tag

@RestController
@RequestMapping("/api/alunos")
@Tag(name = "Alunos", description = "Endpoints para gerenciamento de alunos")
class AlunoController(private val service: AlunoService) {

    @GetMapping
    fun getAll(): List<Aluno> = service.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Aluno> {
        val aluno = service.findById(id)
        return if (aluno != null) {
            ResponseEntity.ok(aluno)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody aluno: Aluno): Aluno = service.save(aluno)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody aluno: Aluno): ResponseEntity<Aluno> {
        val updated = service.update(id, aluno)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
