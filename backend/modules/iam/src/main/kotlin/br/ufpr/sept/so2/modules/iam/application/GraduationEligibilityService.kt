package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import java.util.UUID

data class EligibilityBlock(
    val razao: String,
    val detalhe: String,
)

data class EligibilityResult(
    val alunoId: UUID,
    val eligible: Boolean,
    val bloqueios: List<EligibilityBlock>,
)

@Service
class GraduationEligibilityService(
    private val usuarioRepo: UsuarioJpaRepository,
) {
    @PersistenceContext
    private lateinit var em: EntityManager

    fun evaluate(alunoId: UUID): EligibilityResult {
        val usuario =
            usuarioRepo.findByIdWithRoles(alunoId).orElseThrow {
                NoSuchElementException("Aluno não encontrado: $alunoId")
            }
        return evaluate(usuario)
    }

    fun evaluate(usuario: UsuarioEntity): EligibilityResult {
        val bloqueios = mutableListOf<EligibilityBlock>()
        if (!usuario.ativo || usuario.grr.isNullOrBlank()) {
            bloqueios += EligibilityBlock("CADASTRO", "Aluno inativo ou sem GRR.")
        }
        if (usuario.usuarioRoles.any { it.role.code == "EGRESSO" }) {
            bloqueios += EligibilityBlock("EGRESSO", "Aluno já possui role EGRESSO.")
        }
        if (!hasApprovedTcc(usuario.id)) {
            bloqueios += EligibilityBlock("TCC", "TCC não está APROVADO.")
        }
        val hist = historicoCompleto(usuario)
        if (hist != null) {
            bloqueios += hist
        }
        val horas = horasFormativas(usuario)
        if (horas != null) {
            bloqueios += horas
        }
        if (usuario.metadata["pendenciaFinanceira"] == true ||
            usuario.metadata["pendencia_financeira"] == true
        ) {
            bloqueios += EligibilityBlock("FINANCEIRO", "Há pendência financeira no cadastro.")
        }
        if (hasBlockingRequests(usuario.id)) {
            bloqueios +=
                EligibilityBlock(
                    "SOLICITACOES",
                    "Existe solicitação em ABERTA, EM_DELIBERACAO ou EM_AJUSTE.",
                )
        }
        return EligibilityResult(usuario.id, bloqueios.isEmpty(), bloqueios)
    }

    private fun hasApprovedTcc(alunoId: UUID): Boolean {
        val n =
            em.createNativeQuery(
                """
                SELECT COUNT(*) FROM tcc t
                JOIN tcc_member m ON m.id_tcc = t.id
                WHERE m.id_aluno = :id AND (t.estado = 'APROVADO' OR t.aprovado = TRUE)
                """.trimIndent(),
            ).setParameter("id", alunoId).singleResult as Number
        return n.toLong() > 0
    }

    private fun historicoCompleto(usuario: UsuarioEntity): EligibilityBlock? {
        val cursoId = courseIdOf(usuario) ?: return EligibilityBlock("HISTORICO", "Curso do aluno não informado no cadastro.")
        val totalDisc =
            em.createNativeQuery(
                "SELECT COUNT(*) FROM disciplina WHERE id_curso = :curso AND ativa = TRUE",
            ).setParameter("curso", cursoId).singleResult as Number
        if (totalDisc.toLong() == 0L) {
            return EligibilityBlock("HISTORICO", "Curso sem disciplinas ativas cadastradas.")
        }
        val concluidas =
            em.createNativeQuery(
                """
                SELECT COUNT(*) FROM historico_escolar h
                JOIN disciplina d ON d.id = h.id_disciplina
                WHERE h.id_aluno = :aluno AND d.id_curso = :curso AND h.estado = 'CONCLUIDA'
                """.trimIndent(),
            ).setParameter("aluno", usuario.id).setParameter("curso", cursoId).singleResult as Number
        if (concluidas.toLong() < totalDisc.toLong()) {
            return EligibilityBlock(
                "HISTORICO",
                "Histórico incompleto: ${concluidas.toLong()} de ${totalDisc.toLong()} disciplinas CONCLUIDA.",
            )
        }
        return null
    }

    private fun horasFormativas(usuario: UsuarioEntity): EligibilityBlock? {
        val cursoId = courseIdOf(usuario)
        val minimo =
            if (cursoId != null) {
                (
                    em.createNativeQuery(
                        "SELECT COALESCE(horas_formativas_minimas, 120) FROM curso WHERE id = :id",
                    ).setParameter("id", cursoId).resultList.firstOrNull() as? Number
                )?.toDouble() ?: 120.0
            } else {
                120.0
            }
        val horas =
            (
                em.createNativeQuery(
                    "SELECT COALESCE(SUM(horas_aprovadas), 0) FROM formative_entry WHERE id_aluno = :id",
                ).setParameter("id", usuario.id).singleResult as Number
            ).toDouble()
        if (horas < minimo) {
            return EligibilityBlock("HORAS_FORMATIVAS", "Horas formativas $horas < mínimo $minimo.")
        }
        return null
    }

    private fun hasBlockingRequests(alunoId: UUID): Boolean {
        val n =
            em.createNativeQuery(
                """
                SELECT COUNT(*) FROM request
                WHERE id_solicitante = :id
                  AND deleted_at IS NULL
                  AND estado IN ('ABERTA', 'EM_DELIBERACAO', 'EM_AJUSTE')
                """.trimIndent(),
            ).setParameter("id", alunoId).singleResult as Number
        return n.toLong() > 0
    }

    fun courseIdOf(usuario: UsuarioEntity): UUID? {
        val raw = usuario.metadata["idCurso"] ?: usuario.metadata["id_curso"] ?: return null
        return runCatching { UUID.fromString(raw.toString()) }.getOrNull()
    }
}
