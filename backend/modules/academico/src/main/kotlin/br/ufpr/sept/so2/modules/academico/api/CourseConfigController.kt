package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class UpdateCourseConfigDto(
    @field:Min(0) @field:Max(1000) val horasFormativasMinimas: Int? = null,
    val duracaoCalendario: String? = null,
    @field:Min(1) @field:Max(2) val bancaMembrosExternos: Int? = null,
    val bancaModalidade: String? = null,
    @field:Size(max = 10000) val regimento: String? = null,
)

@RestController
@RequestMapping("/courses")
@Tag(name = "Coordenação — Configuração do curso", description = "Parâmetros acadêmicos (horas, banca, regimento)")
class CourseConfigController(
    private val cursoRepo: CursoJpaRepository,
    private val auditPublisher: AuditPublisher,
) {
    @GetMapping("/{id}/config")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('system.admin')")
    @Operation(summary = "Carregar configuração do curso (coordenador dono)")
    fun get(
        @PathVariable id: String,
    ): Map<String, Any?> {
        val curso = resolve(id)
        assertOwner(curso)
        return curso.toConfigMap(includeUpdate = true)
    }

    @PatchMapping("/{id}/config")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('system.admin')")
    @Operation(summary = "Atualizar configuração — não recalcula elegibilidades já concedidas")
    @Transactional
    fun patch(
        @PathVariable id: String,
        @Valid @RequestBody dto: UpdateCourseConfigDto,
        http: HttpServletRequest,
    ): ResponseEntity<Map<String, Any?>> {
        val curso = resolve(id)
        assertOwner(curso)
        dto.duracaoCalendario?.let {
            require(it in setOf("15_SEMANAS", "18_SEMANAS")) { "duracaoCalendario deve ser 15_SEMANAS ou 18_SEMANAS." }
        }
        dto.bancaModalidade?.let {
            require(it in setOf("PRESENCIAL", "REMOTO", "HÍBRIDO", "HIBRIDO")) {
                "bancaModalidade deve ser PRESENCIAL, REMOTO ou HÍBRIDO."
            }
        }
        val before =
            mapOf(
                "horasFormativasMinimas" to curso.horasFormativasMinimas,
                "duracaoCalendario" to curso.duracaoCalendario,
                "bancaMembrosExternos" to curso.bancaMembrosExternos,
                "bancaModalidade" to curso.bancaModalidade,
                "regimento" to curso.regimento,
            )
        dto.horasFormativasMinimas?.let { curso.horasFormativasMinimas = it }
        dto.duracaoCalendario?.let { curso.duracaoCalendario = it }
        dto.bancaMembrosExternos?.let { curso.bancaMembrosExternos = it }
        dto.bancaModalidade?.let { curso.bancaModalidade = if (it == "HIBRIDO") "HÍBRIDO" else it }
        dto.regimento?.let { curso.regimento = it }
        cursoRepo.save(curso)
        val after =
            mapOf(
                "horasFormativasMinimas" to curso.horasFormativasMinimas,
                "duracaoCalendario" to curso.duracaoCalendario,
                "bancaMembrosExternos" to curso.bancaMembrosExternos,
                "bancaModalidade" to curso.bancaModalidade,
                "regimento" to curso.regimento,
            )
        auditPublisher.publish(
            AuditPayload(
                acao = "COURSE_CONFIG_UPDATED",
                idAtor = currentUser().userId,
                alvoTipo = "curso",
                alvoId = curso.id,
                ip = http.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim() ?: http.remoteAddr,
                userAgent = http.getHeader("User-Agent"),
                resultado = "OK",
                detalhes = mapOf("de" to before, "para" to after),
            ),
        )
        return ResponseEntity.ok(curso.toConfigMap(includeUpdate = true))
    }

    private fun resolve(id: String): CursoEntity {
        val byUuid = runCatching { UUID.fromString(id) }.getOrNull()
        return if (byUuid != null) {
            cursoRepo.findById(byUuid).orElseThrow { NoSuchElementException("Curso não encontrado: $id") }
        } else {
            cursoRepo.findBySigla(id.uppercase()).orElseThrow { NoSuchElementException("Curso não encontrado: $id") }
        }
    }

    private fun assertOwner(curso: CursoEntity) {
        val user = currentUser()
        if (user.authorities.contains("system.admin")) return
        if (curso.idCoordenador != user.userId) {
            throw AccessDeniedException("Você não é coordenador deste curso.")
        }
    }

    private fun CursoEntity.toConfigMap(includeUpdate: Boolean): Map<String, Any?> {
        val links = mutableMapOf<String, String>("self" to "/courses/$id/config")
        if (includeUpdate) links["update"] = "/courses/$id/config"
        return mapOf(
            "courseId" to id,
            "sigla" to sigla,
            "horasFormativasMinimas" to horasFormativasMinimas,
            "duracaoCalendario" to duracaoCalendario,
            "bancaMembrosExternos" to bancaMembrosExternos,
            "bancaModalidade" to bancaModalidade,
            "regimento" to regimento,
            "_links" to links,
        )
    }
}
