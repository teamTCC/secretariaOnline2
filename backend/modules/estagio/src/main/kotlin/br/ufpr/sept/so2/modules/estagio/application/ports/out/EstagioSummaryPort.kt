package br.ufpr.sept.so2.modules.estagio.application.ports.out

/**
 * Read-only port exposing internship summary data for BFF aggregators.
 */
interface EstagioSummaryPort {
    /** System-wide count of internships in a given state — used by academic summary. */
    fun countByEstado(estado: String): Long

    fun countSemSupervisor(): Long
}
