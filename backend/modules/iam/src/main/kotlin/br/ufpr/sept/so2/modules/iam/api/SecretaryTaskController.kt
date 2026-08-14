package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SecretaryTaskEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.SecretaryTaskJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

data class CreateTaskDto(
    @field:NotBlank val titulo: String,
    val descricao: String? = null,
    val prioridade: String = "NORMAL",
    val prazoEm: OffsetDateTime? = null,
    val idAssignee: UUID? = null,
)

data class PatchTaskDto(
    val titulo: String? = null,
    val descricao: String? = null,
    val estado: String? = null,
    val prioridade: String? = null,
    val idAssignee: UUID? = null,
    val prazoEm: OffsetDateTime? = null,
)

@RestController
@RequestMapping("/tasks")
@Tag(name = "Secretaria — Tarefas", description = "Kanban interno da secretaria")
@PreAuthorize("hasAuthority('task.manage') or hasAuthority('system.admin')")
class SecretaryTaskController(
    private val taskRepo: SecretaryTaskJpaRepository,
) {
    @GetMapping
    @Operation(summary = "Listar tarefas (filtro opcional por estado)")
    fun list(
        @RequestParam(required = false) estado: String?,
        @PageableDefault(size = 50) pageable: Pageable,
    ): PageResponse<Map<String, Any?>> {
        val page = if (estado != null) taskRepo.findAllByEstado(estado.uppercase(), pageable) else taskRepo.findAll(pageable)
        return PageResponse.of(page) { t -> t.toMap() }
    }

    @PostMapping
    @Operation(summary = "Criar tarefa")
    fun create(
        @Valid @RequestBody dto: CreateTaskDto,
    ): ResponseEntity<Map<String, Any?>> {
        val saved =
            taskRepo.save(
                SecretaryTaskEntity(
                    titulo = dto.titulo,
                    descricao = dto.descricao,
                    prioridade = dto.prioridade.uppercase(),
                    prazoEm = dto.prazoEm,
                    idAssignee = dto.idAssignee,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toMap())
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Mover coluna / atualizar tarefa (kanban)")
    fun patch(
        @PathVariable id: UUID,
        @RequestBody dto: PatchTaskDto,
    ): ResponseEntity<Map<String, Any?>> {
        val task = taskRepo.findById(id).orElseThrow { NoSuchElementException("Tarefa não encontrada: $id") }
        dto.titulo?.let { task.titulo = it }
        dto.descricao?.let { task.descricao = it }
        dto.estado?.let {
            val novo = it.uppercase()
            require(novo in ESTADOS) { "Estado inválido: $novo" }
            task.estado = novo
        }
        dto.prioridade?.let { task.prioridade = it.uppercase() }
        dto.idAssignee?.let { task.idAssignee = it }
        dto.prazoEm?.let { task.prazoEm = it }
        taskRepo.save(task)
        return ResponseEntity.ok(task.toMap())
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir tarefa — apenas PENDENTE")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val task = taskRepo.findById(id).orElseThrow { NoSuchElementException("Tarefa não encontrada: $id") }
        require(task.estado == "PENDENTE") { "Só é possível excluir tarefas PENDENTE." }
        taskRepo.delete(task)
        return ResponseEntity.noContent().build()
    }

    private fun SecretaryTaskEntity.toMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "titulo" to titulo,
            "descricao" to descricao,
            "estado" to estado,
            "prioridade" to prioridade,
            "idAssignee" to idAssignee,
            "prazoEm" to prazoEm,
        )

    companion object {
        private val ESTADOS = setOf("PENDENTE", "EM_ANDAMENTO", "CONCLUIDA")
    }
}
