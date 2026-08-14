package br.ufpr.sept.so2.modules.solicitacoes.api

import br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence.RequestJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/publico/solicitacoes")
@Tag(name = "Público", description = "Consulta pública de protocolos de solicitações acadêmicas")
class PublicoSolicitacaoController(
    private val requestRepo: RequestJpaRepository,
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
    ): ResponseEntity<Map<String, Any?>> {
        val request =
            requestRepo
                .findByNumeroAnualAndAno(numero, ano)
                .orElseThrow { NoSuchElementException("Protocolo não encontrado: $ano/$numero") }

        return ResponseEntity.ok(
            mapOf(
                "protocolo" to "$ano/${numero.toString().padStart(4, '0')}",
                "tipo" to request.requestTypeCode,
                "estado" to request.estado,
                "abertaEm" to request.createdAt,
                "prazoEm" to request.prazoEm,
                "_links" to
                    mapOf(
                        "self" to "/publico/solicitacoes/$ano/$numero",
                    ),
            ),
        )
    }
}
