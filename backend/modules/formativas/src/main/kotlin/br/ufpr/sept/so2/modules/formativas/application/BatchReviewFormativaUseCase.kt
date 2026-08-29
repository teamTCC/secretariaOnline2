package br.ufpr.sept.so2.modules.formativas.application

import br.ufpr.sept.so2.modules.formativas.domain.FormativaBusinessException
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryEntity
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
import br.ufpr.sept.so2.modules.presenca.application.CertificateIssuerService
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class BatchReviewFormativaCommand(
    val ids: List<UUID>,
    val acao: String,
    val parecer: String?,
    val revisorId: UUID,
)

data class BatchReviewFormativaResult(
    val processadas: Int,
    val estado: String,
)

@Service
@Transactional
class BatchReviewFormativaUseCase(
    private val activityRepo: FormativeActivityJpaRepository,
    private val entryRepo: FormativeEntryJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val certificateIssuer: CertificateIssuerService,
) {
    fun execute(command: BatchReviewFormativaCommand): BatchReviewFormativaResult {
        val novoEstado =
            when (command.acao.uppercase()) {
                "APROVAR" -> "APROVADA"
                "REJEITAR" -> "REJEITADA"
                else -> throw FormativaBusinessException("Ação inválida: ${command.acao}. Use APROVAR ou REJEITAR.")
            }

        val activities = activityRepo.findAllById(command.ids)
        activities.forEach { a ->
            val wasPending = a.estado == "PENDENTE"
            a.estado = novoEstado
            a.parecerRevisor = command.parecer
            a.idRevisor = command.revisorId
            if (novoEstado == "APROVADA" && wasPending) {
                entryRepo.save(
                    FormativeEntryEntity(
                        idAluno = a.idAluno,
                        idActivity = a.id,
                        horasAprovadas = a.cargaHoraria,
                        aprovadoEm = OffsetDateTime.now(),
                    ),
                )
                certificateIssuer.issueFormativeCertificate(
                    alunoId = a.idAluno,
                    activityId = a.id,
                    titulo = a.titulo,
                    chCreditadas = a.cargaHoraria,
                )
            }
        }
        activityRepo.saveAll(activities)

        outboxPublisher.enqueue(
            eventType = "formativas.batch_revisada",
            aggregateType = "FormativeActivity",
            aggregateId = command.revisorId,
            payload =
                mapOf(
                    "ids" to command.ids.map { it.toString() },
                    "acao" to command.acao.uppercase(),
                    "novoEstado" to novoEstado,
                    "revisorId" to command.revisorId.toString(),
                    "parecer" to (command.parecer ?: ""),
                ),
        )

        return BatchReviewFormativaResult(processadas = activities.size, estado = novoEstado)
    }
}
