package br.ufpr.sept.so2.modules.presenca.api

import br.ufpr.sept.so2.modules.presenca.api.dto.CertificateVerificationResponse
import br.ufpr.sept.so2.modules.presenca.application.PublicoCertificateQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/publico")
@Tag(name = "Público", description = "Endpoints de verificação pública (sem autenticação)")
class PublicoController(
    private val publicoCertificateQuery: PublicoCertificateQuery,
) {
    @GetMapping("/verificar-certificado/{hash}")
    @SecurityRequirements
    @Operation(
        summary = "Verificar autenticidade de certificado (anti-fraude)",
        description = "Confere SHA-256 do PDF no MinIO e a assinatura Ed25519 do hash.",
    )
    fun verificarCertificado(
        @PathVariable hash: String,
    ): CertificateVerificationResponse = publicoCertificateQuery.verificarCertificado(hash)
}
