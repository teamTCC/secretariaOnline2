package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.domain.exceptions.AccountBlockedException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.InvalidCredentialsException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.InvalidTokenException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.PasswordReuseException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.WeakPasswordException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.OffsetDateTime

@RestControllerAdvice
class IamExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ProblemDetail =
        problem(
            HttpStatus.UNAUTHORIZED,
            "Não autorizado",
            ex.message ?: "Credenciais inválidas. Verifique seus dados e tente novamente.",
            "unauthorized",
        )

    @ExceptionHandler(AccountBlockedException::class)
    fun handleAccountBlocked(ex: AccountBlockedException): ProblemDetail =
        problem(
            HttpStatus.UNAUTHORIZED,
            "Não autorizado",
            "Credenciais inválidas. Verifique seus dados e tente novamente.",
            "unauthorized",
        )

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, "Token inválido", ex.message ?: "Token inválido ou expirado", "unauthorized")

    @ExceptionHandler(WeakPasswordException::class)
    fun handleWeakPassword(ex: WeakPasswordException): ProblemDetail =
        problem(HttpStatus.UNPROCESSABLE_ENTITY, "Senha fraca", ex.message ?: "Senha não atende os requisitos mínimos", "weak-password")

    @ExceptionHandler(PasswordReuseException::class)
    fun handlePasswordReuse(ex: PasswordReuseException): ProblemDetail =
        problem(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Senha já utilizada",
            "Esta senha já foi utilizada recentemente.",
            "password-reuse",
        )

    private fun problem(
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
