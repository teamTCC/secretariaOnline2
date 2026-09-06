package br.ufpr.sept.so2.modules.notificacoes.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OutboxEventSummaryResponse(
    val id: UUID,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: UUID?,
    val status: String,
    val retryCount: Int,
    val createdAt: OffsetDateTime?,
    val sentAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OutboxDeadEventResponse(
    val id: UUID,
    val eventType: String,
    val aggregateId: UUID?,
    val retryCount: Int,
    val lastError: String?,
    val createdAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OutboxEventDetailResponse(
    val id: UUID,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: UUID?,
    val payload: Any?,
    val status: String,
    val retryCount: Int,
    val lastError: String?,
    val nextAttemptAt: OffsetDateTime?,
    val sentAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OutboxAdminResponse(val mensagem: String, val id: UUID)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OutboxRetryResponse(val id: UUID, val status: String)
