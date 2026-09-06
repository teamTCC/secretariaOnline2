package br.ufpr.sept.so2.modules.solicitacoes.application.ports.out

import java.util.UUID

/**
 * Port for generating one-time-use JWT deep-link tokens for workflow notifications.
 *
 * Implemented by the IAM module adapter, keeping solicitacoes domain decoupled
 * from JWT infrastructure details.
 *
 * Use case: after a REQUEST_ADJUSTMENT transition, the student receives an email
 * with a direct link containing this token so they can access the request page
 * without needing to manually log in and navigate.
 *
 * The token is single-use (JTI blacklisted on first verification) with a 3-day TTL.
 * The audience claim is "request:<requestId>" to scope it to a specific request.
 */
interface OneTimeTokenPort {
    /**
     * Issues a short-lived one-time JWT for [subjectId] (the notification recipient),
     * scoped to the given [requestId].
     *
     * @return compact JWT string ready to embed in a deep-link URL query param `?ott=<token>`
     */
    fun issueForRequest(subjectId: UUID, requestId: UUID): String
}
