package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.api.dto.RequestTypeDetailResponse
import br.ufpr.sept.so2.modules.solicitacoes.api.dto.UpsertRequestTypeDto
import br.ufpr.sept.so2.modules.solicitacoes.application.ManageRequestTypeUseCase
import br.ufpr.sept.so2.modules.solicitacoes.application.RequestTypeQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/request-types")
@Tag(name = "Admin — Tipos de Solicitação", description = "Editor do catálogo RequestType (ADR-003)")
class AdminRequestTypeController(
    private val requestTypeQuery: RequestTypeQuery,
    private val manageRequestTypeUseCase: ManageRequestTypeUseCase,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin') or hasAuthority('request.view_curso')")
    @Operation(summary = "Listar tipos de solicitação (inclui rascunhos para admin)")
    fun list(): List<RequestTypeDetailResponse> = requestTypeQuery.list()

    @PostMapping
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Criar tipo em rascunho (ativo=false)")
    fun create(
        @Valid @RequestBody dto: UpsertRequestTypeDto,
    ): ResponseEntity<RequestTypeDetailResponse> {
        val id =
            manageRequestTypeUseCase.create(
                code = dto.code,
                descricao = dto.descricao,
                formSchema = dto.formSchema,
                workflowJson = dto.workflowJson,
                prazoDias = dto.prazoDias,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(requestTypeQuery.getById(id))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Atualizar rascunho de tipo (form_schema / workflow_json)")
    fun update(
        @PathVariable id: UUID,
        @RequestBody dto: UpsertRequestTypeDto,
    ): ResponseEntity<RequestTypeDetailResponse> {
        val updatedId = manageRequestTypeUseCase.update(id, dto.descricao, dto.formSchema, dto.workflowJson, dto.prazoDias)
        return ResponseEntity.ok(requestTypeQuery.getById(updatedId))
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Publicar tipo — passa a aparecer em GET /requests/types")
    fun publish(
        @PathVariable id: UUID,
    ): ResponseEntity<RequestTypeDetailResponse> {
        val publishedId = manageRequestTypeUseCase.publish(id)
        return ResponseEntity.ok(requestTypeQuery.getById(publishedId))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Excluir tipo sem histórico de solicitações")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        manageRequestTypeUseCase.delete(id)
        return ResponseEntity.noContent().build()
    }
}
