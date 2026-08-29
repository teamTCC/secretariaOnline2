package br.ufpr.sept.so2.modules.solicitacoes.application.ports.out

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Read-only port exposing the minimal slice of solicitações data
 * needed by BFF aggregators. All methods return plain DTOs — no JPA
 * entities or Spring Data types leak beyond this boundary.
 */
data class RequestCardDto(
    val id: UUID,
    val tipo: String,
    val estado: String,
    val prazoEm: OffsetDateTime?,
    val createdAt: OffsetDateTime,
)

interface SolicitacaoDashboardPort {
    /** Requests in EM_AJUSTE state awaiting resubmission by the student. */
    fun findPendenciasAluno(alunoId: UUID, limit: Int): List<RequestCardDto>

    /** Most recent requests opened by the student, any state. */
    fun findRecentesAluno(alunoId: UUID, limit: Int): List<RequestCardDto>

    /** Requests awaiting deliberation (EM_DELIBERACAO), system-wide. */
    fun findPendentesDeliberacao(limit: Int): List<RequestCardDto>

    /** Total count for a given estado (used by secretary KPIs and summary). */
    fun countByEstado(estado: String): Long
}
