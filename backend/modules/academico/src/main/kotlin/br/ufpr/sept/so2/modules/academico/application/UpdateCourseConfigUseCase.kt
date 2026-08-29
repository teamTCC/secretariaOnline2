package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoEntity
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.CursoJpaRepository
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UpdateCourseConfigCommand(
    val courseId: String,
    val horasFormativasMinimas: Int?,
    val duracaoCalendario: String?,
    val bancaMembrosExternos: Int?,
    val bancaModalidade: String?,
    val regimento: String?,
    val requestingUserId: UUID,
    val requestingUserAuthorities: Collection<String>,
    val ip: String,
    val userAgent: String?,
)

@Service
@Transactional
class UpdateCourseConfigUseCase(
    private val cursoRepo: CursoJpaRepository,
    private val auditPublisher: AuditPublisher,
) {
    fun execute(command: UpdateCourseConfigCommand): UUID {
        val curso = resolveCurso(command.courseId)
        assertOwner(curso, command.requestingUserId, command.requestingUserAuthorities)

        command.duracaoCalendario?.let {
            require(it in setOf("15_SEMANAS", "18_SEMANAS")) {
                "duracaoCalendario deve ser 15_SEMANAS ou 18_SEMANAS."
            }
        }
        command.bancaModalidade?.let {
            require(it in setOf("PRESENCIAL", "REMOTO", "HÍBRIDO", "HIBRIDO")) {
                "bancaModalidade deve ser PRESENCIAL, REMOTO ou HÍBRIDO."
            }
        }

        val before = captureConfig(curso)
        command.horasFormativasMinimas?.let { curso.horasFormativasMinimas = it }
        command.duracaoCalendario?.let { curso.duracaoCalendario = it }
        command.bancaMembrosExternos?.let { curso.bancaMembrosExternos = it }
        command.bancaModalidade?.let { curso.bancaModalidade = if (it == "HIBRIDO") "HÍBRIDO" else it }
        command.regimento?.let { curso.regimento = it }

        val saved = cursoRepo.save(curso)
        val after = captureConfig(saved)

        auditPublisher.publish(
            AuditPayload(
                acao = "COURSE_CONFIG_UPDATED",
                idAtor = command.requestingUserId,
                alvoTipo = "curso",
                alvoId = saved.id,
                ip = command.ip,
                userAgent = command.userAgent,
                resultado = "OK",
                detalhes = mapOf("de" to before, "para" to after),
            ),
        )
        return saved.id
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

    private fun captureConfig(c: CursoEntity) =
        mapOf(
            "horasFormativasMinimas" to c.horasFormativasMinimas,
            "duracaoCalendario" to c.duracaoCalendario,
            "bancaMembrosExternos" to c.bancaMembrosExternos,
            "bancaModalidade" to c.bancaModalidade,
            "regimento" to c.regimento,
        )
}
