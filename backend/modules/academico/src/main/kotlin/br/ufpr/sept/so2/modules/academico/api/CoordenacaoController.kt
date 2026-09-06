package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.api.dto.CreateDisciplinaDto
import br.ufpr.sept.so2.modules.academico.api.dto.CreatePeriodoDto
import br.ufpr.sept.so2.modules.academico.api.dto.CursoDetailResponse
import br.ufpr.sept.so2.modules.academico.api.dto.CursoUpdatedResponse
import br.ufpr.sept.so2.modules.academico.api.dto.DisciplinaCreatedResponse
import br.ufpr.sept.so2.modules.academico.api.dto.PeriodoLetivoCreatedResponse
import br.ufpr.sept.so2.modules.academico.api.dto.PeriodoLetivoSummaryResponse
import br.ufpr.sept.so2.modules.academico.api.dto.UpdateCursoDto
import br.ufpr.sept.so2.modules.academico.application.CoordenacaoQuery
import br.ufpr.sept.so2.modules.academico.application.CreateDisciplinaCommand
import br.ufpr.sept.so2.modules.academico.application.CreatePeriodoCommand
import br.ufpr.sept.so2.modules.academico.application.ManageCursoUseCase
import br.ufpr.sept.so2.modules.academico.application.ManagePeriodoLetivoUseCase
import br.ufpr.sept.so2.modules.academico.application.UpdateCursoCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/academico")
@Tag(name = "Coordenação", description = "Endpoints de gestão de cursos, disciplinas e períodos para Coordenadores")
class CoordenacaoController(
    private val coordenacaoQuery: CoordenacaoQuery,
    private val manageCursoUseCase: ManageCursoUseCase,
    private val managePeriodoLetivoUseCase: ManagePeriodoLetivoUseCase,
) {
    @GetMapping("/cursos/{id}")
    @Operation(summary = "Detalhe de um curso")
    @PreAuthorize("isAuthenticated()")
    fun getCurso(
        @PathVariable id: UUID,
    ): CursoDetailResponse = coordenacaoQuery.getCurso(id)

    @PatchMapping("/cursos/{id}")
    @Operation(summary = "Atualizar nome ou sigla de um curso (Coordenador)")
    @PreAuthorize("hasAuthority('user.manage_students')")
    fun updateCurso(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateCursoDto,
    ): ResponseEntity<CursoUpdatedResponse> {
        val saved = manageCursoUseCase.updateCurso(UpdateCursoCommand(cursoId = id, nome = dto.nome, sigla = dto.sigla))
        return ResponseEntity.ok(CursoUpdatedResponse(id = saved.id, nome = saved.nome, sigla = saved.sigla))
    }

    @PostMapping("/disciplinas")
    @Operation(summary = "Criar disciplina em um curso (Coordenador)")
    @PreAuthorize("hasAuthority('user.manage_students')")
    fun createDisciplina(
        @Valid @RequestBody dto: CreateDisciplinaDto,
    ): ResponseEntity<DisciplinaCreatedResponse> {
        val saved =
            manageCursoUseCase.createDisciplina(
                CreateDisciplinaCommand(
                    idCurso = dto.idCurso,
                    codigo = dto.codigo,
                    nome = dto.nome,
                    cargaHorariaTotal = dto.cargaHorariaTotal,
                    creditos = dto.creditos,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            DisciplinaCreatedResponse(id = saved.id, codigo = saved.codigo, nome = saved.nome),
        )
    }

    @GetMapping("/periodos-letivos")
    @Operation(summary = "Listar todos os períodos letivos")
    @PreAuthorize("isAuthenticated()")
    fun listPeriodos(): List<PeriodoLetivoSummaryResponse> = coordenacaoQuery.listPeriodos()

    @PostMapping("/periodos-letivos")
    @Operation(summary = "Criar novo período letivo (Coordenador)")
    @PreAuthorize("hasAuthority('user.manage_students')")
    fun createPeriodo(
        @Valid @RequestBody dto: CreatePeriodoDto,
    ): ResponseEntity<PeriodoLetivoCreatedResponse> {
        val saved =
            managePeriodoLetivoUseCase.create(
                CreatePeriodoCommand(
                    ano = dto.ano,
                    semestre = dto.semestre,
                    inicio = dto.inicio,
                    fim = dto.fim,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            PeriodoLetivoCreatedResponse(id = saved.id, ano = saved.ano, semestre = saved.semestre),
        )
    }
}
