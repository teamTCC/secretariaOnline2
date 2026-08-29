package br.ufpr.sept.so2.modules.iam.api.dto

import jakarta.validation.constraints.NotBlank

// ─── Request DTOs ──────────────────────────────────────────────────────────

data class CreateTicketRequest(
    @field:NotBlank val assunto: String,
    @field:NotBlank val descricao: String,
)

data class RespondTicketRequest(
    @field:NotBlank val resposta: String,
)

data class CreateFaqItemRequest(
    @field:NotBlank val categoria: String,
    @field:NotBlank val pergunta: String,
    @field:NotBlank val resposta: String,
    val ordem: Int = 0,
)

data class UpdateFaqItemRequest(
    val pergunta: String?,
    val resposta: String?,
    val ordem: Int?,
    val ativo: Boolean?,
)
