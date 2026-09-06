package br.ufpr.sept.so2.modules.comunicacao.api

import br.ufpr.sept.so2.modules.comunicacao.api.dto.CreateTemplateDto
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateCreatedResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateRevisedResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateRevisionDetailResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateRevisionDto
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateRevisionSummaryResponse
import br.ufpr.sept.so2.modules.comunicacao.api.dto.TemplateSummaryResponse
import br.ufpr.sept.so2.modules.comunicacao.application.CommunicationTemplateQuery
import br.ufpr.sept.so2.modules.comunicacao.application.CreateTemplateCommand
import br.ufpr.sept.so2.modules.comunicacao.application.CreateTemplateUseCase
import br.ufpr.sept.so2.modules.comunicacao.application.ReviseTemplateCommand
import br.ufpr.sept.so2.modules.comunicacao.application.ReviseTemplateUseCase
import br.ufpr.sept.so2.shared.security.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/communication-templates")
@Tag(name = "Admin — Templates", description = "Catálogo versionado de templates de e-mail/in-app")
@PreAuthorize("hasAuthority('communication.manage_templates') or hasAuthority('system.admin')")
class CommunicationTemplateController(
    private val communicationTemplateQuery: CommunicationTemplateQuery,
    private val createTemplateUseCase: CreateTemplateUseCase,
    private val reviseTemplateUseCase: ReviseTemplateUseCase,
) {
    @GetMapping
    @Operation(summary = "Listar templates")
    fun list(): List<TemplateSummaryResponse> = communicationTemplateQuery.list()

    @PostMapping
    @Operation(summary = "Criar template (versão 1)")
    fun create(
        @Valid @RequestBody dto: CreateTemplateDto,
    ): ResponseEntity<TemplateCreatedResponse> {
        val result =
            createTemplateUseCase.execute(
                CreateTemplateCommand(
                    codigo = dto.codigo,
                    titulo = dto.titulo,
                    assunto = dto.assunto,
                    corpo = dto.corpo,
                    canal = dto.canal,
                    autorId = currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            TemplateCreatedResponse(id = result.id, codigo = result.codigo, versao = result.versao),
        )
    }

    @PostMapping("/{id}/revisions")
    @Operation(summary = "Nova revisão — incrementa versão (imutável)")
    fun revise(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: TemplateRevisionDto,
    ): ResponseEntity<TemplateRevisedResponse> {
        val result =
            reviseTemplateUseCase.execute(
                ReviseTemplateCommand(
                    id = id,
                    assunto = dto.assunto,
                    corpo = dto.corpo,
                    autorId = currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            TemplateRevisedResponse(id = result.id, versao = result.versao),
        )
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Histórico de versões")
    fun versions(
        @PathVariable id: UUID,
    ): List<TemplateRevisionSummaryResponse> = communicationTemplateQuery.versions(id)

    @GetMapping("/{id}/versions/{rev}")
    @Operation(summary = "Detalhe de uma revisão")
    fun version(
        @PathVariable id: UUID,
        @PathVariable rev: Int,
    ): TemplateRevisionDetailResponse = communicationTemplateQuery.version(id, rev)
}
