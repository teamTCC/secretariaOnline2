package br.ufpr.sept.so2.modules.solicitacoes.domain

import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class WorkflowEngine(
    private val definition: WorkflowDefinition,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun allowedTransitions(
        currentState: RequestState,
        authorities: Set<String>,
    ): List<WorkflowDefinition.Transition> =
        definition.transitions
            .filter { it.from == currentState.name }
            .filter { transition -> transition.requiresAuthority.any { auth -> auth in authorities } }

    fun applyTransition(
        request: Request,
        action: String,
        actorId: UUID,
        actorAuthorities: Set<String>,
        parecer: String?,
    ): RequestTransitionResult {
        val transition =
            definition.transitions.find {
                it.from == request.estado.name && it.action == action
            } ?: throw InvalidTransitionException(
                from = request.estado.name,
                action = action,
                requestId = request.id,
            )

        if (transition.requiresAuthority.none { it in actorAuthorities }) {
            throw InsufficientAuthorityException(
                required = transition.requiresAuthority,
                action = action,
            )
        }

        if (transition.guard != null) {
            evaluateGuard(transition.guard, request, actorId)
        }

        val newState = RequestState.valueOf(transition.to)
        val event =
            RequestEvent(
                id = UUID.randomUUID(),
                idRequest = request.id,
                tipo = action,
                estadoAnterior = request.estado,
                estadoNovo = newState,
                idAtor = actorId,
                parecer = parecer,
                createdAt = OffsetDateTime.now(),
            )

        log.debug("Transição aplicada: {} → {} na solicitação {}", request.estado, newState, request.id)

        return RequestTransitionResult(
            newState = newState,
            event = event,
            notifyTemplate = transition.notifyTemplate,
            generateOneTimeToken = transition.generateOneTimeToken,
        )
    }

    private fun evaluateGuard(
        guard: String,
        request: Request,
        actorId: UUID,
    ) {
        // Simple guard evaluation: actor.id == request.idSolicitante
        if (guard.contains("actor.id == request.idSolicitante")) {
            if (actorId != request.idSolicitante) {
                throw TransitionGuardFailedException(guard, "Apenas o solicitante pode executar esta ação.")
            }
        }
        if (guard.contains("request.allowsReview")) {
            if (!request.allowsReview()) {
                throw TransitionGuardFailedException(guard, "Esta solicitação não permite revisão.")
            }
        }
    }
}

class InvalidTransitionException(
    from: String,
    action: String,
    requestId: UUID,
) : IllegalStateException("Transição '$action' não é válida a partir do estado '$from' na solicitação $requestId")

class InsufficientAuthorityException(
    required: List<String>,
    action: String,
) : org.springframework.security.access.AccessDeniedException(
        "Ação '$action' requer uma das seguintes capacidades: ${required.joinToString()}",
    )

class TransitionGuardFailedException(
    guard: String,
    reason: String,
) : IllegalStateException("Guard '$guard' falhou: $reason")
