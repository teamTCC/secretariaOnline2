package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.api.dto.ContactInfoResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ContactMessageAcceptedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ContatoFormDto
import br.ufpr.sept.so2.modules.iam.application.ContatoPublicoQuery
import br.ufpr.sept.so2.modules.iam.application.SubmitContactCommand
import br.ufpr.sept.so2.modules.iam.application.SubmitContactUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/publico/contato")
@Tag(name = "Público", description = "Página de contato da secretaria")
class ContatoPublicoController(
    private val contatoPublicoQuery: ContatoPublicoQuery,
    private val submitContactUseCase: SubmitContactUseCase,
) {
    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Dados estáticos de contato da secretaria")
    fun get(): ContactInfoResponse = contatoPublicoQuery.get()

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Enviar mensagem de contato (pública, rate-limited)")
    fun send(
        @Valid @RequestBody dto: ContatoFormDto,
    ): ResponseEntity<ContactMessageAcceptedResponse> {
        val accepted =
            submitContactUseCase.execute(
                SubmitContactCommand(
                    nome = dto.nome,
                    email = dto.email,
                    assunto = dto.assunto,
                    mensagem = dto.mensagem,
                ),
            )
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted)
    }
}
