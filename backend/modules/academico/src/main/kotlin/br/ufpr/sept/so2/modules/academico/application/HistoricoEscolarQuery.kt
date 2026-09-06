package br.ufpr.sept.so2.modules.academico.application

import br.ufpr.sept.so2.modules.academico.api.dto.HistoricoItemResponse
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.DisciplinaJpaRepository
import br.ufpr.sept.so2.modules.academico.infrastructure.persistence.HistoricoEscolarJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HistoricoEscolarQuery(
    private val historicoRepo: HistoricoEscolarJpaRepository,
    private val disciplinaRepo: DisciplinaJpaRepository,
) {
    fun list(alunoId: UUID): List<HistoricoItemResponse> =
        historicoRepo.findAllByIdAluno(alunoId).map { h ->
            val disc = disciplinaRepo.findById(h.idDisciplina).orElse(null)
            HistoricoItemResponse(
                id = h.id,
                idDisciplina = h.idDisciplina,
                codigo = disc?.codigo,
                nome = disc?.nome,
                estado = h.estado,
            )
        }
}
