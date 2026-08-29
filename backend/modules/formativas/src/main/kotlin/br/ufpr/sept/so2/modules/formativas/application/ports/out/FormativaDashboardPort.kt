package br.ufpr.sept.so2.modules.formativas.application.ports.out

import java.util.UUID

/**
 * Read-only port exposing formative-hours data for BFF aggregators.
 */
interface FormativaDashboardPort {
    /** Sum of approved hours for a student, 0.0 if none. */
    fun sumHorasAprovadas(alunoId: UUID): Double
}
