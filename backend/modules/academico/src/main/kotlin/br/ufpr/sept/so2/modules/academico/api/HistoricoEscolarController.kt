package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.HistoricoEscolarEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.HistoricoEscolarJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class UpsertHistoricoDto(
    @field:NotBlank val estado: String,
)

@RestController
@RequestMapping("/academico/alunos")
@Tag(name = "Coordenação — Histórico escolar", description = "Disciplinas CONCLUIDA/CURSANDO/REPROVADA por aluno")
class HistoricoEscolarController(
    private val historicoRepo: HistoricoEscolarJpaRepository,
    private val disciplinaRepo: DisciplinaJpaRepository,
) {
    @GetMapping("/{alunoId}/historico")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Listar histórico escolar do aluno")
    fun list(
        @PathVariable alunoId: UUID,
    ): List<Map<String, Any?>> =
        historicoRepo.findAllByIdAluno(alunoId).map { h ->
            val disc = disciplinaRepo.findById(h.idDisciplina).orElse(null)
            mapOf(
                "id" to h.id,
                "idDisciplina" to h.idDisciplina,
                "codigo" to disc?.codigo,
                "nome" to disc?.nome,
                "estado" to h.estado,
            )
        }

    @PutMapping("/{alunoId}/historico/{disciplinaId}")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('diploma.register') or hasAuthority('system.admin')")
    @Operation(summary = "Upsert estado de uma disciplina no histórico")
    fun upsert(
        @PathVariable alunoId: UUID,
        @PathVariable disciplinaId: UUID,
        @Valid @RequestBody dto: UpsertHistoricoDto,
    ): ResponseEntity<Map<String, Any?>> {
        val estado = dto.estado.uppercase()
        require(estado in setOf("CURSANDO", "CONCLUIDA", "REPROVADA")) {
            "estado deve ser CURSANDO, CONCLUIDA ou REPROVADA."
        }
        disciplinaRepo.findById(disciplinaId).orElseThrow { NoSuchElementException("Disciplina não encontrada: $disciplinaId") }
        val entity =
            historicoRepo.findByIdAlunoAndIdDisciplina(alunoId, disciplinaId).orElse(
                HistoricoEscolarEntity(idAluno = alunoId, idDisciplina = disciplinaId, estado = estado),
            )
        entity.estado = estado
        val saved = historicoRepo.save(entity)
        return ResponseEntity.ok(mapOf("id" to saved.id, "idAluno" to alunoId, "idDisciplina" to disciplinaId, "estado" to saved.estado))
    }
}
