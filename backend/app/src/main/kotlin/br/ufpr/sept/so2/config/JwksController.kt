package br.ufpr.sept.so2.config

import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.modules.presenca.config.CertificateProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@Tag(name = "Público", description = "JWKS para verificação offline de JWT (RSA) e certificados (Ed25519)")
class JwksController(
    private val jwtTokenService: JwtTokenService,
    private val certProperties: CertificateProperties,
) {
    @GetMapping("/.well-known/jwks.json")
    @SecurityRequirements
    @Operation(
        summary = "JWKS — chaves públicas RSA (JWT) e Ed25519 (certificados)",
        description = "Endpoint público. Não exige Bearer token.",
    )
    fun jwks(): Map<String, Any> {
        val pubKey = jwtTokenService.publicKey
        val jwtKey =
            mapOf(
                "kty" to "RSA",
                "use" to "sig",
                "alg" to "RS256",
                "kid" to "jwt-signing-key-1",
                "n" to Base64.getUrlEncoder().withoutPadding().encodeToString(pubKey.modulus.toByteArray()),
                "e" to Base64.getUrlEncoder().withoutPadding().encodeToString(pubKey.publicExponent.toByteArray()),
            )

        val keys = mutableListOf<Map<String, Any>>(jwtKey)

        if (certProperties.publicKey.isNotBlank()) {
            val decoded = runCatching { Base64.getDecoder().decode(certProperties.publicKey) }.getOrNull()
            val raw =
                when {
                    decoded == null -> ByteArray(0)
                    decoded.size >= 32 -> decoded.copyOfRange(decoded.size - 32, decoded.size)
                    else -> decoded
                }
            keys +=
                mapOf(
                    "kty" to "OKP",
                    "crv" to "Ed25519",
                    "use" to "sig",
                    "kid" to "cert-signing-key-1",
                    "x" to Base64.getUrlEncoder().withoutPadding().encodeToString(raw),
                )
        }

        return mapOf("keys" to keys)
    }
}
