package br.ufpr.sept.so2.modules.iam.api

import br.ufpr.sept.so2.modules.iam.config.ContatoProperties
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ContactMessageEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.ContactMessageJpaRepository
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class ContatoFormDto(
    @field:NotBlank @field:Size(max = 200) val nome: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(max = 300) val assunto: String,
    @field:NotBlank @field:Size(max = 4000) val mensagem: String,
)

@RestController
@RequestMapping("/publico/contato")
@Tag(name = "Público", description = "Página de contato da secretaria")
class ContatoPublicoController(
    private val props: ContatoProperties,
    private val contactRepo: ContactMessageJpaRepository,
    private val outboxPublisher: OutboxEventPublisher,
) {
    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Dados estáticos de contato da secretaria")
    fun get(): Map<String, Any> =
        mapOf(
            "nome" to props.nome,
            "endereco" to props.endereco,
            "telefone" to props.telefone,
            "email" to props.email,
            "horario" to props.horario,
            "_links" to mapOf("enviar" to "/publico/contato"),
        )

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Enviar mensagem de contato (pública, rate-limited)")
    fun send(
        @Valid @RequestBody dto: ContatoFormDto,
    ): ResponseEntity<Map<String, Any>> {
        val saved =
            contactRepo.save(
                ContactMessageEntity(
                    nome = dto.nome.trim(),
                    email = dto.email.trim().lowercase(),
                    assunto = dto.assunto.trim(),
                    mensagem = dto.mensagem.trim(),
                ),
            )
        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.CONTATO_RECEBIDO,
            aggregateType = "ContactMessage",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "nome" to saved.nome,
                    "email" to saved.email,
                    "assunto" to saved.assunto,
                    "mensagem" to saved.mensagem,
                ),
        )
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf("id" to saved.id, "status" to "ACEITO", "mensagem" to "Mensagem recebida. Retornaremos por e-mail."),
        )
    }
}
