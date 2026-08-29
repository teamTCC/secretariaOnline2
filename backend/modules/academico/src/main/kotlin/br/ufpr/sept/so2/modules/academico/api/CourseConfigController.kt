package br.ufpr.sept.so2.modules.academico.api

import br.ufpr.sept.so2.modules.academico.api.dto.CourseConfigResponse
import br.ufpr.sept.so2.modules.academico.api.dto.UpdateCourseConfigDto
import br.ufpr.sept.so2.modules.academico.application.CourseConfigQuery
import br.ufpr.sept.so2.modules.academico.application.UpdateCourseConfigCommand
import br.ufpr.sept.so2.modules.academico.application.UpdateCourseConfigUseCase
import br.ufpr.sept.so2.shared.security.currentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/courses")
@Tag(name = "Coordenação — Configuração do curso", description = "Parâmetros acadêmicos (horas, banca, regimento)")
class CourseConfigController(
    private val courseConfigQuery: CourseConfigQuery,
    private val updateCourseConfigUseCase: UpdateCourseConfigUseCase,
) {
    @GetMapping("/{id}/config")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('system.admin')")
    @Operation(summary = "Carregar configuração do curso (coordenador dono)")
    fun get(
        @PathVariable id: String,
    ): CourseConfigResponse {
        val user = currentUser()
        return courseConfigQuery.get(id, user.userId, user.authorities)
    }

    @PatchMapping("/{id}/config")
    @PreAuthorize("hasAuthority('course.config') or hasAuthority('system.admin')")
    @Operation(summary = "Atualizar configuração — não recalcula elegibilidades já concedidas")
    fun patch(
        @PathVariable id: String,
        @Valid @RequestBody dto: UpdateCourseConfigDto,
        http: HttpServletRequest,
    ): ResponseEntity<CourseConfigResponse> {
        val user = currentUser()
        val ip = http.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim() ?: http.remoteAddr
        updateCourseConfigUseCase.execute(
            UpdateCourseConfigCommand(
                courseId = id,
                horasFormativasMinimas = dto.horasFormativasMinimas,
                duracaoCalendario = dto.duracaoCalendario,
                bancaMembrosExternos = dto.bancaMembrosExternos,
                bancaModalidade = dto.bancaModalidade,
                regimento = dto.regimento,
                requestingUserId = user.userId,
                requestingUserAuthorities = user.authorities,
                ip = ip,
                userAgent = http.getHeader("User-Agent"),
            ),
        )
        return ResponseEntity.ok(courseConfigQuery.get(id, user.userId, user.authorities))
    }
}
