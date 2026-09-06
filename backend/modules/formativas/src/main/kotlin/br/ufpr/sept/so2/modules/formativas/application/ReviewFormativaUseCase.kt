package br.ufpr.sept.so2.modules.formativas.application

import br.ufpr.sept.so2.modules.formativas.domain.FormativaBusinessException
import br.ufpr.sept.so2.modules.formativas.domain.FormativaNotFoundException
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeActivityJpaRepository
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryEntity
import br.ufpr.sept.so2.modules.formativas.infrastructure.persistence.FormativeEntryJpaRepository
import br.ufpr.sept.so2.modules.presenca.application.CertificateIssuerService
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class ReviewFormativaCommand(
    val id: UUID,
    val revisorId: UUID,
    val acao: String,
    val parecer: String?,
)

data class ReviewFormativaResult(
    val estado: String,
)

@Service
@Transactional
class ReviewFormativaUseCase(
    private val activityRepo: FormativeActivityJpaRepository,
    private val entryRepo: FormativeEntryJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val certificateIssuer: CertificateIssuerService,
) {
    fun execute(command: ReviewFormativaCommand): ReviewFormativaResult {
        val activity =
            activityRepo
                .findById(command.id)
                .orElseThrow { FormativaNotFoundException(command.id) }

        if (activity.estado != "PENDENTE") {
            throw FormativaBusinessException("Atividade não está pendente de revisão.")
        }

        activity.estado =
            when (command.acao.uppercase()) {
                "APROVAR" -> "APROVADA"
                "REJEITAR" -> "REJEITADA"
                else -> throw FormativaBusinessException("Ação inválida: ${command.acao}")
            }
        activity.parecerRevisor = command.parecer
        activity.idRevisor = command.revisorId
        activityRepo.save(activity)

        if (activity.estado == "APROVADA" && !entryRepo.existsByIdActivity(activity.id)) {
            entryRepo.save(
                FormativeEntryEntity(
                    idAluno = activity.idAluno,
                    idActivity = activity.id,
                    horasAprovadas = activity.cargaHoraria,
                    aprovadoEm = OffsetDateTime.now(),
                ),
            )
            certificateIssuer.issueFormativeCertificate(
                alunoId = activity.idAluno,
                activityId = activity.id,
                titulo = activity.titulo,
                chCreditadas = activity.cargaHoraria,
            )
        }

        outboxPublisher.enqueue(
            eventType = "formativas.revisada",
            aggregateType = "FormativeActivity",
            aggregateId = activity.id,
            payload =
                mapOf(
                    "activityId" to activity.id.toString(),
                    "idAluno" to activity.idAluno.toString(),
                    "estado" to activity.estado,
                    "parecer" to (activity.parecerRevisor ?: ""),
                ),
        )

        return ReviewFormativaResult(estado = activity.estado)
    }
}
