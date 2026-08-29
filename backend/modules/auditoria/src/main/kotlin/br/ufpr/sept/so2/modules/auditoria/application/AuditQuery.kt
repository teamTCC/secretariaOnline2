package br.ufpr.sept.so2.modules.auditoria.application

import br.ufpr.sept.so2.modules.auditoria.api.dto.AuditLogSummaryResponse
import br.ufpr.sept.so2.modules.auditoria.infrastructure.persistence.AuditLogJpaRepository
import br.ufpr.sept.so2.shared.api.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class AuditQuery(
    private val auditLogRepo: AuditLogJpaRepository,
) {
    fun query(
        idAtor: UUID?,
        acao: String?,
        resultado: String?,
        since: OffsetDateTime?,
        pageable: Pageable,
    ): PageResponse<AuditLogSummaryResponse> {
        val desde = since ?: OffsetDateTime.now().minusDays(30)
        val page = auditLogRepo.findWithFilters(idAtor, acao, resultado, desde, pageable)
        return PageResponse.ofWithLinks(page) { a ->
            AuditLogSummaryResponse(
                id = a.id,
                entityType = a.alvoTipo,
                entityId = a.alvoId,
                action = a.acao,
                actorId = a.idAtor,
                actorEmail = null,
                details = mapOf("ip" to a.ip, "userAgent" to a.userAgent, "resultado" to a.resultado),
                createdAt = a.at,
            )
        }
    }
}
