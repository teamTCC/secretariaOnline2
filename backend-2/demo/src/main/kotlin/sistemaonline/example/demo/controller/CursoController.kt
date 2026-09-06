package sistemaonline.example.demo.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import sistemaonline.example.demo.model.Curso
import sistemaonline.example.demo.service.CursoService

@RestController
@RequestMapping("/api/cursos")
@Tag(name = "Cursos", description = "Endpoints para gerenciamento de cursos")
class CursoController(private val service: CursoService) {

    @GetMapping
    fun getAll(): List<Curso> = service.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Curso> {
        val curso = service.findById(id)
        return if (curso != null) {
            ResponseEntity.ok(curso)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody curso: Curso): Curso = service.save(curso)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody curso: Curso): ResponseEntity<Curso> {
        val updated = service.update(id, curso)
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
