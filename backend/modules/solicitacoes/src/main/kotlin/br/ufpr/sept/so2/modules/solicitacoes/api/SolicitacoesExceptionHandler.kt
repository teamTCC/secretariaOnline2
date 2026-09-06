package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.domain.InsufficientAuthorityException
import br.ufpr.sept.so2.modules.solicitacoes.domain.InvalidTransitionException
import br.ufpr.sept.so2.modules.solicitacoes.domain.SchemaValidationException
import br.ufpr.sept.so2.modules.solicitacoes.domain.TransitionGuardFailedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.OffsetDateTime

/**
 * Module-local exception handler for solicitacoes-specific exceptions.
 * Registered alongside the global handler; Spring picks the most specific match first.
 * This avoids coupling the shared GlobalExceptionHandler to this module.
 */
@RestControllerAdvice(basePackages = ["br.ufpr.sept.so2.modules.solicitacoes"])
class SolicitacoesExceptionHandler {
    /**
     * RFC 7807 — 422 when dados payload does not conform to form_schema.
     * Provides the list of schema validation errors in the response body.
     */
    @ExceptionHandler(SchemaValidationException::class)
    fun handleSchemaValidation(ex: SchemaValidationException): ProblemDetail =
        problemDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Payload inválido segundo o form_schema",
            "Os dados enviados não estão de acordo com o esquema do tipo de solicitação.",
            "schema-validation-error",
        ).also { it.setProperty("erros", ex.errors) }

    /**
     * RFC 7807 — 422 when the requested action is not a valid transition from the current state.
     */
    @ExceptionHandler(InvalidTransitionException::class)
    fun handleInvalidTransition(ex: InvalidTransitionException): ProblemDetail =
        problemDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Transição inválida",
            ex.message ?: "A transição solicitada não é permitida no estado atual.",
            "invalid-transition",
        )

    @ExceptionHandler(InsufficientAuthorityException::class)
    fun handleInsufficientAuthority(ex: InsufficientAuthorityException): ProblemDetail =
        problemDetail(
            HttpStatus.FORBIDDEN,
            "Acesso negado",
            ex.message ?: "Você não tem autoridade para esta transição.",
            "insufficient-authority",
        )

    /**
     * RFC 7807 — 403 when the actor does not satisfy the guard expression
     * (e.g. actor.id != request.idSolicitante for RESUBMIT).
     */
    @ExceptionHandler(TransitionGuardFailedException::class)
    fun handleGuardFailed(ex: TransitionGuardFailedException): ProblemDetail =
        problemDetail(
            HttpStatus.FORBIDDEN,
            "Condição não satisfeita",
            ex.message ?: "Você não atende às condições necessárias para executar esta transição.",
            "transition-guard-failed",
        )

    private fun problemDetail(
        status: HttpStatus,
        title: String,
        detail: String,
        type: String,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.title = title
            this.type = URI.create("https://secretariaonline.ufpr.br/errors/$type")
            setProperty("timestamp", OffsetDateTime.now().toString())
        }
}
