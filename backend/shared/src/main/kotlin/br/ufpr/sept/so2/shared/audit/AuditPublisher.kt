package br.ufpr.sept.so2.shared.audit

import java.util.UUID

interface AuditPublisher {
    fun publish(event: AuditPayload)
}

data class AuditPayload(
    val acao: String,
    val idAtor: UUID?,
    val alvoTipo: String?,
    val alvoId: UUID?,
    val ip: String?,
    val userAgent: String?,
    val resultado: String,
    val detalhes: Map<String, Any?> = emptyMap(),
)
