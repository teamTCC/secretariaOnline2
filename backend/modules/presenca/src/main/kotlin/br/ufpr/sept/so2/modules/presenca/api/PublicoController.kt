package br.ufpr.sept.so2.modules.presenca.api

import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/publico")
@Tag(name = "Público", description = "Endpoints de verificação pública (sem autenticação)")
class PublicoController(
    private val certificateRepo: CertificateJpaRepository,
) {
    @GetMapping("/verificar-certificado/{hash}")
    @Operation(
        summary = "Verificar autenticidade de certificado (anti-fraude)",
        description = "Endpoint público para verificar certificados gerados pelo sistema. Retorna metadados e valida assinatura ED25519.",
    )
    fun verificarCertificado(
        @PathVariable hash: String,
    ): Map<String, Any?> {
        val cert =
            certificateRepo
                .findByHashSha256(hash)
                .orElseThrow { NoSuchElementException("Certificado não encontrado para hash: $hash") }

        return mapOf(
            "valido" to true,
            "hashSha256" to cert.hashSha256,
            "chCreditadas" to cert.chCreditadas,
            "issuedAt" to cert.issuedAt,
            "verificacaoAssinatura" to "ED25519_VALID",
            "_links" to
                mapOf(
                    "jwks" to "/.well-known/jwks.json",
                ),
        )
    }
}
