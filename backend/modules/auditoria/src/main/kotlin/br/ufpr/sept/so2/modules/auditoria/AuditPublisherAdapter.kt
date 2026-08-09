package br.ufpr.sept.so2.modules.auditoria

import br.ufpr.sept.so2.modules.auditoria.infrastructure.persistence.AuditLogEntity
import br.ufpr.sept.so2.modules.auditoria.infrastructure.persistence.AuditLogJpaRepository
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuditPublisherAdapter(
    private val auditLogRepo: AuditLogJpaRepository,
) : AuditPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: AuditPayload) {
        try {
            auditLogRepo.save(
                AuditLogEntity(
                    idAtor = event.idAtor,
                    acao = event.acao,
                    alvoTipo = event.alvoTipo,
                    alvoId = event.alvoId,
                    ip = event.ip,
                    userAgent = event.userAgent,
                    resultado = event.resultado,
                    payload = event.detalhes,
                ),
            )
        } catch (e: Exception) {
            log.error("Falha ao persistir audit log para acao={}: {}", event.acao, e.message, e)
        }
    }
}
