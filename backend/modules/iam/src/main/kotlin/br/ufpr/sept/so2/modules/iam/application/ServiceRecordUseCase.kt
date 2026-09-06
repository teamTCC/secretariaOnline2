package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.ServiceRecordResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ServiceRecordEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ServiceRecordJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class CreateServiceRecordCommand(
    val idAluno: UUID,
    val assunto: String,
    val descricao: String? = null,
    val tipo: String = "PRESENCIAL",
    val idSecretario: UUID,
    val clientIp: String?,
    val userAgent: String?,
)

data class ScheduleServiceRecordCommand(
    val idAluno: UUID,
    val assunto: String,
    val descricao: String? = null,
    val tipo: String = "AGENDAMENTO",
    val clientIp: String?,
    val userAgent: String?,
)

data class AcknowledgeServiceRecordCommand(
    val recordId: UUID,
    val alunoId: UUID,
    val clientIp: String?,
    val userAgent: String?,
)

@Service
@Transactional
class ServiceRecordUseCase(
    private val serviceRecordRepo: ServiceRecordJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
    private val auditPublisher: AuditPublisher,
) {
    fun create(cmd: CreateServiceRecordCommand): ServiceRecordResponse {
        val entity =
            ServiceRecordEntity(
                idSecretario = cmd.idSecretario,
                idAluno = cmd.idAluno,
                tipo = cmd.tipo,
                assunto = cmd.assunto,
                descricao = cmd.descricao,
                estado = "PENDENTE_CIENCIA",
            )
        val saved = serviceRecordRepo.save(entity)
        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.ATENDIMENTO_CRIADO,
            aggregateType = "ServiceRecord",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "alunoId" to saved.idAluno.toString(),
                    "assunto" to saved.assunto,
                    "tipo" to saved.tipo,
                ),
        )
        auditPublisher.publish(
            AuditPayload(
                acao = "SERVICE_RECORD_CREATED",
                idAtor = cmd.idSecretario,
                alvoTipo = "service_record",
                alvoId = saved.id,
                ip = cmd.clientIp,
                userAgent = cmd.userAgent,
                resultado = "OK",
            ),
        )
        return saved.toResponse(includeAcknowledge = false)
    }

    fun schedule(cmd: ScheduleServiceRecordCommand): ServiceRecordResponse {
        val entity =
            ServiceRecordEntity(
                idSecretario = null,
                idAluno = cmd.idAluno,
                tipo = cmd.tipo.ifBlank { "AGENDAMENTO" },
                assunto = cmd.assunto,
                descricao = cmd.descricao,
                estado = "AGENDADO",
            )
        val saved = serviceRecordRepo.save(entity)
        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.ATENDIMENTO_CRIADO,
            aggregateType = "ServiceRecord",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "alunoId" to saved.idAluno.toString(),
                    "assunto" to saved.assunto,
                    "tipo" to saved.tipo,
                ),
        )
        auditPublisher.publish(
            AuditPayload(
                acao = "SERVICE_RECORD_SCHEDULED",
                idAtor = cmd.idAluno,
                alvoTipo = "service_record",
                alvoId = saved.id,
                ip = cmd.clientIp,
                userAgent = cmd.userAgent,
                resultado = "OK",
            ),
        )
        return saved.toResponse(includeAcknowledge = false)
    }

    fun acknowledge(cmd: AcknowledgeServiceRecordCommand): ServiceRecordResponse {
        val rec =
            serviceRecordRepo.findById(cmd.recordId)
                .orElseThrow { NoSuchElementException("Atendimento não encontrado: ${cmd.recordId}") }
        if (rec.idAluno != cmd.alunoId) {
            throw AccessDeniedException("Acesso negado ao atendimento ${cmd.recordId}")
        }
        require(rec.estado == "PENDENTE_CIENCIA") { "Atendimento já possui ciência (estado=${rec.estado})." }
        rec.estado = "CIENTE"
        rec.acknowledgedAt = OffsetDateTime.now()
        serviceRecordRepo.save(rec)
        auditPublisher.publish(
            AuditPayload(
                acao = "SERVICE_RECORD_ACKNOWLEDGED",
                idAtor = cmd.alunoId,
                alvoTipo = "service_record",
                alvoId = rec.id,
                ip = cmd.clientIp,
                userAgent = cmd.userAgent,
                resultado = "OK",
            ),
        )
        return rec.toResponse(includeAcknowledge = false)
    }
}
