package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.api.dto.HistoricoItemResponse
import br.ufpr.sept.so2.modules.academico.api.dto.HistoricoUpsertResponse
import br.ufpr.sept.so2.modules.academico.api.dto.UpsertHistoricoDto
import br.ufpr.sept.so2.modules.academico.application.HistoricoEscolarQuery
import br.ufpr.sept.so2.modules.academico.application.UpsertHistoricoCommand
import br.ufpr.sept.so2.modules.academico.application.UpsertHistoricoEscolarUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/academico/alunos")
@Tag(name = "Coordenação — Histórico escolar", description = "Disciplinas CONCLUIDA/CURSANDO/REPROVADA por aluno")
class HistoricoEscolarController(
    private val historicoEscolarQuery: HistoricoEscolarQuery,
    private val upsertHistoricoEscolarUseCase: UpsertHistoricoEscolarUseCase,
) {
    @GetMapping("/{alunoId}/historico")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Listar histórico escolar do aluno")
    fun list(
        @PathVariable alunoId: UUID,
    ): List<HistoricoItemResponse> = historicoEscolarQuery.list(alunoId)

    @PutMapping("/{alunoId}/historico/{disciplinaId}")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Upsert estado de uma disciplina no histórico")
    fun upsert(
        @PathVariable alunoId: UUID,
        @PathVariable disciplinaId: UUID,
        @Valid @RequestBody dto: UpsertHistoricoDto,
    ): ResponseEntity<HistoricoUpsertResponse> {
        val saved =
            upsertHistoricoEscolarUseCase.execute(
                UpsertHistoricoCommand(
                    alunoId = alunoId,
                    disciplinaId = disciplinaId,
                    estado = dto.estado,
                ),
            )
        return ResponseEntity.ok(
            HistoricoUpsertResponse(
                id = saved.id,
                idAluno = alunoId,
                idDisciplina = disciplinaId,
                estado = saved.estado,
            ),
        )
    }
}
