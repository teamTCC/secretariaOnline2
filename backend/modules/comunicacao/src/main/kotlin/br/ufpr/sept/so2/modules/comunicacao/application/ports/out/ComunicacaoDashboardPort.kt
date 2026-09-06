package br.ufpr.sept.so2.modules.comunicacao.application.ports.out

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Read-only port exposing communication-delivery data for BFF aggregators.
 */
data class ComunicadoCardDto(
    val id: UUID,
    val idCommunication: UUID,
    val deliveredAt: OffsetDateTime?,
    val readAt: OffsetDateTime?,
)

interface ComunicacaoDashboardPort {
    /** Most recent deliveries for a user, ordered newest-first. */
    fun findRecentesByUsuario(usuarioId: UUID, limit: Int): List<ComunicadoCardDto>
}
