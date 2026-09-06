package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.application.PublicoSolicitacaoQuery
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicoSolicitacaoResponse(
    val protocolo: String,
    val tipo: String,
    val estado: String,
    val abertaEm: OffsetDateTime?,
    val prazoEm: OffsetDateTime?,
    @JsonProperty("_links") val links: Map<String, String>,
)

@RestController
@RequestMapping("/publico/solicitacoes")
@Tag(name = "Público", description = "Consulta pública de protocolos de solicitações acadêmicas")
class PublicoSolicitacaoController(
    private val publicoSolicitacaoQuery: PublicoSolicitacaoQuery,
) {
    @GetMapping("/{ano}/{numero}")
    @SecurityRequirements
    @Operation(
        summary = "Verificar status público de solicitação por protocolo",
        description = "Consulta pública de protocolo. Exemplo: GET /publico/solicitacoes/2025/42. Retorna apenas dados não-sigilosos.",
    )
    fun verificarProtocolo(
        @PathVariable ano: Short,
        @PathVariable numero: Int,
    ): ResponseEntity<PublicoSolicitacaoResponse> =
        ResponseEntity.ok(publicoSolicitacaoQuery.verificarProtocolo(ano, numero))
}
