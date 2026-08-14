package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CalendarioAcademicoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.PeriodoLetivoJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
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
import java.time.LocalDate
import java.util.UUID

data class UpdateCursoDto(
    val nome: String?,
    val sigla: String?,
)

data class CreateDisciplinaDto(
    val idCurso: UUID,
    @field:NotBlank val codigo: String,
    @field:NotBlank val nome: String,
    @field:Positive val cargaHorariaTotal: Int,
    @field:Positive val creditos: Int,
)

data class CreatePeriodoDto(
    @field:Positive val ano: Short,
    @field:Positive val semestre: Short,
    val inicio: LocalDate,
    val fim: LocalDate,
)

@RestController
@RequestMapping("/academico")
@Tag(name = "Coordenação", description = "Endpoints de gestão de cursos, disciplinas e períodos para Coordenadores")
class CoordenacaoController(
    private val cursoRepo: CursoJpaRepository,
    private val disciplinaRepo: DisciplinaJpaRepository,
    private val periodoRepo: PeriodoLetivoJpaRepository,
    private val calendarioRepo: CalendarioAcademicoJpaRepository,
) {
    @GetMapping("/cursos/{id}")
    @Operation(summary = "Detalhe de um curso")
    @PreAuthorize("isAuthenticated()")
    fun getCurso(
        @PathVariable id: UUID,
    ): Map<String, Any?> {
        val c = cursoRepo.findById(id).orElseThrow { NoSuchElementException("Curso não encontrado: $id") }
        return mapOf(
            "id" to c.id,
            "nome" to c.nome,
            "sigla" to c.sigla,
            "idCoordenador" to c.idCoordenador,
            "ativo" to c.ativo,
            "horasFormativasMinimas" to c.horasFormativasMinimas,
            "duracaoCalendario" to c.duracaoCalendario,
            "bancaMembrosExternos" to c.bancaMembrosExternos,
            "bancaModalidade" to c.bancaModalidade,
            "_links" to mapOf("config" to "/courses/${c.id}/config"),
        )
    }

    @PatchMapping("/cursos/{id}")
    @Operation(summary = "Atualizar nome ou sigla de um curso (Coordenador)")
    @PreAuthorize("hasAuthority('user.manage_students')")
    fun updateCurso(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateCursoDto,
    ): ResponseEntity<Map<String, Any?>> {
        val curso = cursoRepo.findById(id).orElseThrow { NoSuchElementException("Curso não encontrado: $id") }
        dto.nome?.let { curso.nome = it }
        dto.sigla?.let { curso.sigla = it }
        cursoRepo.save(curso)
        return ResponseEntity.ok(mapOf("id" to curso.id, "nome" to curso.nome, "sigla" to curso.sigla))
    }

    @PostMapping("/disciplinas")
    @Operation(summary = "Criar disciplina em um curso (Coordenador)")
    @PreAuthorize("hasAuthority('user.manage_students')")
    fun createDisciplina(
        @Valid @RequestBody dto: CreateDisciplinaDto,
    ): ResponseEntity<Map<String, Any?>> {
        cursoRepo.findById(dto.idCurso).orElseThrow { NoSuchElementException("Curso não encontrado: ${dto.idCurso}") }
        val entity =
            DisciplinaEntity(
                idCurso = dto.idCurso,
                codigo = dto.codigo,
                nome = dto.nome,
                cargaHorariaTotal = dto.cargaHorariaTotal,
                creditos = dto.creditos,
            )
        val saved = disciplinaRepo.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "codigo" to saved.codigo, "nome" to saved.nome),
        )
    }

    @GetMapping("/periodos-letivos")
    @Operation(summary = "Listar todos os períodos letivos")
    @PreAuthorize("isAuthenticated()")
    fun listPeriodos(): List<Map<String, Any?>> =
        periodoRepo.findAll().map { p ->
            mapOf(
                "id" to p.id,
                "ano" to p.ano,
                "semestre" to p.semestre,
                "inicio" to p.inicio,
                "fim" to p.fim,
                "ativo" to p.ativo,
            )
        }

    @PostMapping("/periodos-letivos")
    @Operation(summary = "Criar novo período letivo (Coordenador)")
    @PreAuthorize("hasAuthority('user.manage_students')")
    fun createPeriodo(
        @Valid @RequestBody dto: CreatePeriodoDto,
    ): ResponseEntity<Map<String, Any?>> {
        require(dto.fim.isAfter(dto.inicio)) { "Data fim deve ser após data início." }
        val entity =
            PeriodoLetivoEntity(
                ano = dto.ano,
                semestre = dto.semestre,
                inicio = dto.inicio,
                fim = dto.fim,
            )
        val saved = periodoRepo.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("id" to saved.id, "ano" to saved.ano, "semestre" to saved.semestre),
        )
    }
}
