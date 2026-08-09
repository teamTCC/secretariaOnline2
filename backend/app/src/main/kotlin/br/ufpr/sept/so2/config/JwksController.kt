package br.ufpr.sept.so2.config

import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
class JwksController(
    private val jwtTokenService: JwtTokenService,
) {
    @GetMapping("/.well-known/jwks.json")
    fun jwks(): Map<String, Any> {
        val pubKey = jwtTokenService.publicKey
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(pubKey.encoded)
        return mapOf(
            "keys" to
                listOf(
                    mapOf(
                        "kty" to "RSA",
                        "use" to "sig",
                        "alg" to "RS256",
                        "n" to Base64.getUrlEncoder().withoutPadding().encodeToString(pubKey.modulus.toByteArray()),
                        "e" to Base64.getUrlEncoder().withoutPadding().encodeToString(pubKey.publicExponent.toByteArray()),
                    ),
                ),
        )
    }
}
