package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeEntity
import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestTypeJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class UpsertRequestTypeDto(
    @field:NotBlank val code: String,
    @field:NotBlank val descricao: String,
    val formSchema: Map<String, Any> = emptyMap(),
    val workflowJson: Map<String, Any> = emptyMap(),
    val prazoDias: Int = 10,
)

@RestController
@RequestMapping("/request-types")
@Tag(name = "Admin — Tipos de Solicitação", description = "Editor do catálogo RequestType (ADR-003)")
class AdminRequestTypeController(
    private val typeRepo: RequestTypeJpaRepository,
    private val requestRepo: RequestJpaRepository,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin') or hasAuthority('request.view_curso')")
    @Operation(summary = "Listar tipos de solicitação (inclui rascunhos para admin)")
    fun list(): List<Map<String, Any?>> =
        typeRepo.findAll().map { it.toMap() }

    @PostMapping
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Criar tipo em rascunho (ativo=false)")
    fun create(
        @Valid @RequestBody dto: UpsertRequestTypeDto,
    ): ResponseEntity<Map<String, Any?>> {
        val code = dto.code.uppercase()
        require(typeRepo.findByCode(code).isEmpty) { "Tipo já existe: $code" }
        require(dto.prazoDias > 0) { "prazoDias deve ser positivo." }
        val saved =
            typeRepo.save(
                RequestTypeEntity(
                    code = code,
                    descricao = dto.descricao,
                    formSchema = dto.formSchema,
                    workflowJson = dto.workflowJson,
                    prazoDias = dto.prazoDias,
                    ativo = false,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toMap())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Atualizar rascunho de tipo (form_schema / workflow_json)")
    @Transactional
    fun update(
        @PathVariable id: UUID,
        @RequestBody dto: UpsertRequestTypeDto,
    ): ResponseEntity<Map<String, Any?>> {
        val entity = typeRepo.findById(id).orElseThrow { NoSuchElementException("Tipo não encontrado: $id") }
        entity.descricao = dto.descricao
        entity.formSchema = dto.formSchema
        entity.workflowJson = dto.workflowJson
        entity.prazoDias = dto.prazoDias
        typeRepo.save(entity)
        return ResponseEntity.ok(entity.toMap())
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Publicar tipo — passa a aparecer em GET /requests/types")
    @Transactional
    fun publish(
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, Any?>> {
        val entity = typeRepo.findById(id).orElseThrow { NoSuchElementException("Tipo não encontrado: $id") }
        require(entity.formSchema.isNotEmpty()) { "formSchema não pode ser vazio para publicar." }
        require(entity.workflowJson.isNotEmpty()) { "workflowJson não pode ser vazio para publicar." }
        entity.ativo = true
        typeRepo.save(entity)
        return ResponseEntity.ok(entity.toMap())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('request_type.manage') or hasAuthority('system.admin')")
    @Operation(summary = "Excluir tipo sem histórico de solicitações")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        typeRepo.findById(id).orElseThrow { NoSuchElementException("Tipo não encontrado: $id") }
        val used = requestRepo.countByIdRequestType(id)
        require(used == 0L) { "Não é possível excluir tipo com $used solicitações no histórico." }
        typeRepo.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    private fun RequestTypeEntity.toMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "code" to code,
            "descricao" to descricao,
            "formSchema" to formSchema,
            "workflowJson" to workflowJson,
            "prazoDias" to prazoDias,
            "ativo" to ativo,
        )
}
