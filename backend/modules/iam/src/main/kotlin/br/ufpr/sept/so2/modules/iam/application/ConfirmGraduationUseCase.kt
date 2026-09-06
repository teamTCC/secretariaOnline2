package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.GraduationRecordJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

data class ConfirmGraduationCommand(
    val alunoIds: List<UUID>,
    val idCurso: UUID? = null,
    val dataColacao: LocalDate? = null,
    val observacao: String? = null,
    val livro: String? = null,
    val folha: String? = null,
    val ata: String? = null,
    val periodoId: UUID? = null,
)

data class ConfirmGraduationResult(
    val processados: Int,
    val registros: Int,
)

@Service
@Transactional
class ConfirmGraduationUseCase(
    private val graduationRepo: GraduationRecordJpaRepository,
    private val usuarioRepo: UsuarioJpaRepository,
    private val roleRepo: RoleJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val eligibilityService: GraduationEligibilityService,
    private val diplomaPdfService: DiplomaPdfService,
) {
    fun execute(cmd: ConfirmGraduationCommand): ConfirmGraduationResult {
        val egressoRole =
            roleRepo.findByCode("EGRESSO").orElseThrow { NoSuchElementException("Role EGRESSO não cadastrada") }
        val records = mutableListOf<GraduationRecordEntity>()

        cmd.alunoIds.forEach { alunoId ->
            val usuario =
                usuarioRepo.findByIdWithRoles(alunoId).orElseThrow {
                    NoSuchElementException("Aluno não encontrado: $alunoId")
                }
            val elig = eligibilityService.evaluate(usuario)
            require(elig.eligible) {
                "Aluno $alunoId não está elegível: ${elig.bloqueios.joinToString("; ") { it.razao + " — " + it.detalhe }}"
            }

            if (!graduationRepo.existsByIdAluno(alunoId)) {
                val rec =
                    graduationRepo.save(
                        GraduationRecordEntity(
                            idAluno = alunoId,
                            idCurso = cmd.idCurso ?: eligibilityService.courseIdOf(usuario),
                            dataColacao = cmd.dataColacao ?: LocalDate.now(),
                            observacao = cmd.observacao,
                            livro = cmd.livro,
                            folha = cmd.folha,
                            ata = cmd.ata,
                            idPeriodo = cmd.periodoId,
                        ),
                    )
                runCatching { diplomaPdfService.generateAndStore(rec) }
                graduationRepo.save(rec)
                records += rec
            }

            val alreadyEgresso = usuario.usuarioRoles.any { it.role.code == "EGRESSO" }
            if (!alreadyEgresso) {
                usuario.usuarioRoles.add(UsuarioRoleEntity(usuario = usuario, role = egressoRole))
                usuarioRepo.save(usuario)
            }
        }

        records.forEach { rec ->
            outboxPublisher.enqueue(
                eventType = OutboxEventTypes.GRADUATION_CONFIRMED,
                aggregateType = "GraduationRecord",
                aggregateId = rec.id,
                payload = mapOf("alunoId" to rec.idAluno.toString()),
            )
        }

        return ConfirmGraduationResult(processados = cmd.alunoIds.size, registros = records.size)
    }
}
