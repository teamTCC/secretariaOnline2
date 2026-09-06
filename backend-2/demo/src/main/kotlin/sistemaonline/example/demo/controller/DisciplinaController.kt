package sistemaonline.example.demo.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import sistemaonline.example.demo.model.Disciplina
import sistemaonline.example.demo.service.DisciplinaService

@RestController
@RequestMapping("/api/disciplinas")
@Tag(name = "Disciplinas", description = "Endpoints para gerenciamento de disciplinas")
class DisciplinaController(private val service: DisciplinaService) {

    @GetMapping
    fun getAll(): List<Disciplina> = service.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Disciplina> {
        val disciplina = service.findById(id)
        return if (disciplina != null) {
            ResponseEntity.ok(disciplina)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody disciplina: Disciplina): Disciplina = service.save(disciplina)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody disciplina: Disciplina): ResponseEntity<Disciplina> {
        val updated = service.update(id, disciplina)
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
