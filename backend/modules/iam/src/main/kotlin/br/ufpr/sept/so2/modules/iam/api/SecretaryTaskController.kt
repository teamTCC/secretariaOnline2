package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.CreateTaskDto
import br.ufpr.sept.so2.modules.iam.api.dto.PatchTaskDto
import br.ufpr.sept.so2.modules.iam.api.dto.SecretaryTaskResponse
import br.ufpr.sept.so2.modules.iam.application.CreateSecretaryTaskCommand
import br.ufpr.sept.so2.modules.iam.application.ManageSecretaryTaskUseCase
import br.ufpr.sept.so2.modules.iam.application.PatchSecretaryTaskCommand
import br.ufpr.sept.so2.modules.iam.application.SecretaryTaskQuery
import br.ufpr.sept.so2.shared.api.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
import java.util.UUID

@RestController
@RequestMapping("/tasks")
@Tag(name = "Secretaria — Tarefas", description = "Kanban interno da secretaria")
@PreAuthorize("hasAuthority('task.manage') or hasAuthority('system.admin')")
class SecretaryTaskController(
    private val secretaryTaskQuery: SecretaryTaskQuery,
    private val manageSecretaryTaskUseCase: ManageSecretaryTaskUseCase,
) {
    @GetMapping
    @Operation(summary = "Listar tarefas (filtro opcional por estado)")
    fun list(
        @RequestParam(required = false) estado: String?,
        @PageableDefault(size = 50) pageable: Pageable,
    ): PageResponse<SecretaryTaskResponse> = secretaryTaskQuery.list(estado, pageable)

    @PostMapping
    @Operation(summary = "Criar tarefa")
    fun create(
        @Valid @RequestBody dto: CreateTaskDto,
    ): ResponseEntity<SecretaryTaskResponse> {
        val saved =
            manageSecretaryTaskUseCase.create(
                CreateSecretaryTaskCommand(
                    titulo = dto.titulo,
                    descricao = dto.descricao,
                    prioridade = dto.prioridade,
                    prazoEm = dto.prazoEm,
                    idAssignee = dto.idAssignee,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Mover coluna / atualizar tarefa (kanban)")
    fun patch(
        @PathVariable id: UUID,
        @RequestBody dto: PatchTaskDto,
    ): ResponseEntity<SecretaryTaskResponse> {
        val updated =
            manageSecretaryTaskUseCase.patch(
                PatchSecretaryTaskCommand(
                    id = id,
                    titulo = dto.titulo,
                    descricao = dto.descricao,
                    estado = dto.estado,
                    prioridade = dto.prioridade,
                    idAssignee = dto.idAssignee,
                    prazoEm = dto.prazoEm,
                ),
            )
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir tarefa — apenas PENDENTE")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        manageSecretaryTaskUseCase.delete(id)
        return ResponseEntity.noContent().build()
    }
}
