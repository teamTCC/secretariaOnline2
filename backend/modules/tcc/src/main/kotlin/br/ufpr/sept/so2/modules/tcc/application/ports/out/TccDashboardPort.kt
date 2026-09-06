package br.ufpr.sept.so2.modules.tcc.application.ports.out

import java.util.UUID

/**
 * Read-only port exposing TCC data for BFF aggregators.
 */
interface TccDashboardPort {
    /** Number of approved (defended) TCCs for a given student/member. */
    fun countDefendidosByAluno(alunoId: UUID): Int

    /** System-wide count of TCCs in a given state — used by academic summary. */
    fun countByEstado(estado: String): Long
}
