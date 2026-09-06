package br.ufpr.sept.so2.modules.auditoria.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuditLogSummaryResponse(
    val id: UUID?,
    val entityType: String?,
    val entityId: UUID?,
    val action: String?,
    val actorId: UUID?,
    val actorEmail: String?,
    val details: Any?,
    val createdAt: OffsetDateTime?,
)
