package br.ufpr.sept.so2.modules.presenca.application.ports.out

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Read-only port exposing event and certificate data for BFF aggregators.
 * All methods return plain DTOs — no JPA entities or Spring Data types
 * leak beyond this boundary.
 */
data class EventCardDto(
    val id: UUID,
    val titulo: String,
    val estado: String,
    val chCreditadas: Double,
    val inicioEm: OffsetDateTime,
    val fimEm: OffsetDateTime,
)

data class CertificateCardDto(
    val id: UUID,
    val hashSha256: String,
    val issuedAt: OffsetDateTime,
)

interface PresencaDashboardPort {
    /** Events currently EM_ANDAMENTO — students can check in to these. */
    fun findEmAndamento(limit: Int): List<EventCardDto>

    /** Events organized by a specific professor (any state), most recent first. */
    fun findByOrganizador(organizadorId: UUID, limit: Int): List<EventCardDto>

    /** Count of EM_ANDAMENTO events where the given user is organizer — used in professor dashboard KPIs. */
    fun countEmAndamentoPorOrganizador(organizadorId: UUID): Long

    /** Certificates issued for an ex-student (egresso). */
    fun findCertificadosByAluno(alunoId: UUID): List<CertificateCardDto>
}
