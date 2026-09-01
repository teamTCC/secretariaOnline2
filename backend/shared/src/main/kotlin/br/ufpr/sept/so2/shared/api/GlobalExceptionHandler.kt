package br.ufpr.sept.so2.shared.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val errors =
            ex.bindingResult.allErrors.map { error ->
                when (error) {
                    is FieldError -> mapOf("field" to error.field, "message" to (error.defaultMessage ?: "inválido"))
                    else -> mapOf("mensagem" to (error.defaultMessage ?: "inválido"))
                }
            }
        return problemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Dados inválidos",
            detail = "A requisição contém dados inválidos. Verifique os campos.",
            type = "validation-error",
        ).also { it.setProperty("errors", errors) }
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): ProblemDetail =
        problemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Dados inválidos",
            detail = "Corpo da requisição ausente ou inválido.",
            type = "validation-error",
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: WebRequest,
    ): ProblemDetail {
        log.warn("Acesso negado: {} - {}", request.getDescription(false), ex.message)
        return problemDetail(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para esta operação.", "forbidden")
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException): ProblemDetail =
        problemDetail(HttpStatus.UNAUTHORIZED, "Não autenticado", "Token inválido ou expirado.", "unauthorized")

    @ExceptionHandler(NoSuchElementException::class, NoResourceFoundException::class)
    fun handleNotFound(ex: Exception): ProblemDetail =
        problemDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.message ?: "O recurso solicitado não foi encontrado.", "not-found")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(ex: HttpRequestMethodNotSupportedException): ProblemDetail =
        problemDetail(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Método não permitido",
            ex.message ?: "Método HTTP não suportado neste path.",
            "method-not-allowed",
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        problemDetail(HttpStatus.BAD_REQUEST, "Dados inválidos", ex.message ?: "Requisição inválida.", "bad-request")

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ProblemDetail {
        log.error("Estado ilegal: {}", ex.message)
        return problemDetail(HttpStatus.CONFLICT, "Conflito de estado", ex.message ?: "Operação inválida no estado atual.", "conflict")
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: WebRequest,
    ): ProblemDetail {
        val incidentId = "INC-${OffsetDateTime.now().year}-${UUID.randomUUID().toString().replace("-", "").take(4)}"
        log.error("Erro inesperado [{}] em {}: {}", incidentId, request.getDescription(false), ex.message, ex)
        return problemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno",
            "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.",
            "internal-error",
        ).also { it.setProperty("incidentId", incidentId) }
    }

    internal fun problemDetail(
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
