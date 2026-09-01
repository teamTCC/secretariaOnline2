package br.ufpr.sept.so2.modules.comunicacao.api

import br.ufpr.sept.so2.modules.comunicacao.domain.CommunicationBusinessException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.OffsetDateTime

@RestControllerAdvice(basePackages = ["br.ufpr.sept.so2.modules.comunicacao"])
class ComunicacaoExceptionHandler {
    @ExceptionHandler(CommunicationBusinessException::class)
    fun handleBusiness(ex: CommunicationBusinessException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.message ?: "Não foi possível processar o comunicado.").apply {
            title = "Dados inválidos"
            type = URI.create("https://secretariaonline.ufpr.br/errors/unprocessable-entity")
            setProperty("timestamp", OffsetDateTime.now().toString())
        }
}
