package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.api.dto.CourseConfigResponse
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CourseConfigQuery(
    private val cursoRepo: CursoJpaRepository,
) {
    fun get(
        courseId: String,
        userId: UUID,
        authorities: Collection<String>,
    ): CourseConfigResponse {
        val curso = resolveCurso(courseId)
        assertOwner(curso, userId, authorities)
        return toResponse(curso, includeUpdate = true)
    }

    fun toResponse(
        curso: CursoEntity,
        includeUpdate: Boolean,
    ): CourseConfigResponse {
        val links = mutableMapOf<String, String>("self" to "/courses/${curso.id}/config")
        if (includeUpdate) links["update"] = "/courses/${curso.id}/config"
        return CourseConfigResponse(
            courseId = curso.id,
            sigla = curso.sigla,
            horasFormativasMinimas = curso.horasFormativasMinimas,
            duracaoCalendario = curso.duracaoCalendario,
            bancaMembrosExternos = curso.bancaMembrosExternos,
            bancaModalidade = curso.bancaModalidade,
            regimento = curso.regimento,
            links = links,
        )
    }

    private fun resolveCurso(id: String): CursoEntity {
        val byUuid = runCatching { UUID.fromString(id) }.getOrNull()
        return if (byUuid != null) {
            cursoRepo.findById(byUuid).orElseThrow { NoSuchElementException("Curso não encontrado: $id") }
        } else {
            cursoRepo.findBySigla(id.uppercase()).orElseThrow { NoSuchElementException("Curso não encontrado: $id") }
        }
    }

    private fun assertOwner(
        curso: CursoEntity,
        userId: UUID,
        authorities: Collection<String>,
    ) {
        if (authorities.contains("system.admin")) return
        if (curso.idCoordenador != userId) {
            throw AccessDeniedException("Você não é coordenador deste curso.")
        }
    }
}
