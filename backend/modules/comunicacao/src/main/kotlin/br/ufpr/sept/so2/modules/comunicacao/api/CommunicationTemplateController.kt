package br.ufpr.sept.so2.modules.comunicacao.api

import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateJpaRepository
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateRevisionEntity
import br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence.CommunicationTemplateRevisionJpaRepository
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateTemplateDto(
    @field:NotBlank val codigo: String,
    @field:NotBlank val titulo: String,
    @field:NotBlank val assunto: String,
    @field:NotBlank val corpo: String,
    val canal: String = "EMAIL",
)

data class TemplateRevisionDto(
    @field:NotBlank val assunto: String,
    @field:NotBlank val corpo: String,
)

@RestController
@RequestMapping("/communication-templates")
@Tag(name = "Admin — Templates", description = "Catálogo versionado de templates de e-mail/in-app")
@PreAuthorize("hasAuthority('communication.manage_templates') or hasAuthority('system.admin')")
class CommunicationTemplateController(
    private val templateRepo: CommunicationTemplateJpaRepository,
    private val revisionRepo: CommunicationTemplateRevisionJpaRepository,
) {
    @GetMapping
    @Operation(summary = "Listar templates")
    fun list(): List<Map<String, Any?>> =
        templateRepo.findAll().map { t ->
            mapOf(
                "id" to t.id,
                "codigo" to t.codigo,
                "titulo" to t.titulo,
                "assunto" to t.assunto,
                "corpo" to t.corpo,
                "canal" to t.canal,
                "versao" to t.versao,
                "ativo" to t.ativo,
                "variaveis" to listOf("nome", "email", "protocolo", "eventoTitulo"),
            )
        }

    @PostMapping
    @Operation(summary = "Criar template (versão 1)")
    @Transactional
    fun create(
        @Valid @RequestBody dto: CreateTemplateDto,
    ): ResponseEntity<Map<String, Any?>> {
        val codigo = dto.codigo.lowercase()
        require(templateRepo.findByCodigo(codigo).isEmpty) { "Template já existe: $codigo" }
        val saved =
            templateRepo.save(
                CommunicationTemplateEntity(
                    codigo = codigo,
                    titulo = dto.titulo,
                    assunto = dto.assunto,
                    corpo = dto.corpo,
                    canal = dto.canal.uppercase(),
                ),
            )
        revisionRepo.save(
            CommunicationTemplateRevisionEntity(
                idTemplate = saved.id,
                versao = 1,
                assunto = dto.assunto,
                corpo = dto.corpo,
                idAutor = currentUserId(),
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "codigo" to saved.codigo, "versao" to saved.versao),
        )
    }

    @PostMapping("/{id}/revisions")
    @Operation(summary = "Nova revisão — incrementa versão (imutável)")
    @Transactional
    fun revise(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: TemplateRevisionDto,
    ): ResponseEntity<Map<String, Any?>> {
        val template = templateRepo.findById(id).orElseThrow { NoSuchElementException("Template não encontrado: $id") }
        template.versao += 1
        template.assunto = dto.assunto
        template.corpo = dto.corpo
        templateRepo.save(template)
        revisionRepo.save(
            CommunicationTemplateRevisionEntity(
                idTemplate = template.id,
                versao = template.versao,
                assunto = dto.assunto,
                corpo = dto.corpo,
                idAutor = currentUserId(),
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to template.id, "versao" to template.versao),
        )
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Histórico de versões")
    fun versions(
        @PathVariable id: UUID,
    ): List<Map<String, Any?>> =
        revisionRepo.findAllByIdTemplateOrderByVersaoDesc(id).map { r ->
            mapOf("versao" to r.versao, "assunto" to r.assunto, "createdAt" to r.createdAt, "idAutor" to r.idAutor)
        }

    @GetMapping("/{id}/versions/{rev}")
    @Operation(summary = "Detalhe de uma revisão")
    fun version(
        @PathVariable id: UUID,
        @PathVariable rev: Int,
    ): Map<String, Any?> {
        val r =
            revisionRepo.findByIdTemplateAndVersao(id, rev).orElseThrow {
                NoSuchElementException("Revisão $rev não encontrada")
            }
        return mapOf("versao" to r.versao, "assunto" to r.assunto, "corpo" to r.corpo, "createdAt" to r.createdAt)
    }
}
