package br.ufpr.sept.so2.modules.auditoria.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuditLogResponse(
    val id: UUID,
    val acao: String,
    val idAtor: UUID?,
    val ip: String?,
    val userAgent: String?,
    val alvoTipo: String?,
    val alvoId: String?,
    val resultado: String,
    val at: OffsetDateTime,
)
