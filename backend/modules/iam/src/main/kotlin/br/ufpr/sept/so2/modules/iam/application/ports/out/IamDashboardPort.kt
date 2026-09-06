package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.util.UUID

/**
 * Read-only port exposing IAM (user and service-record) data for BFF
 * aggregators. All methods return plain DTOs — no JPA entities or Spring
 * Data types leak beyond this boundary.
 */
data class UsuarioBasicoDto(
    val nome: String,
    val email: String,
    val grr: String?,
)

interface IamDashboardPort {
    /** Count of service records in a given state for a student. */
    fun countAtendimentosPendentes(alunoId: UUID): Long

    /** Minimal user info needed for the egresso dashboard greeting. */
    fun findUsuarioBasico(id: UUID): UsuarioBasicoDto?

    /** Total active students (GRR present) — used by academic summary. */
    fun countAlunosAtivos(): Long

    /** Course ID stored in user metadata — used by presença module to filter events by audience. */
    fun findUserCourseId(userId: UUID): UUID?
}
