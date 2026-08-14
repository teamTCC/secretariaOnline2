package br.ufpr.sept.so2.modules.presenca.api

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.presenca.config.CertificateProperties
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateJpaRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@RestController
@RequestMapping("/publico")
@Tag(name = "Público", description = "Endpoints de verificação pública (sem autenticação)")
class PublicoController(
    private val certificateRepo: CertificateJpaRepository,
    private val certProperties: CertificateProperties,
    private val minioStorageService: MinioStorageService,
) {
    @GetMapping("/verificar-certificado/{hash}")
    @SecurityRequirements
    @Operation(
        summary = "Verificar autenticidade de certificado (anti-fraude)",
        description = "Confere SHA-256 do PDF no MinIO e a assinatura Ed25519 do hash.",
    )
    fun verificarCertificado(
        @PathVariable hash: String,
    ): Map<String, Any?> {
        val cert =
            certificateRepo.findByHashSha256(hash)
                .orElseThrow { NoSuchElementException("Certificado não encontrado para hash: $hash") }

        val hashBytes = hexToBytes(cert.hashSha256)
        val signatureOk = verifySignature(hashBytes, cert.signatureEd25519)
        val artifactOk =
            runCatching {
                val pdf = minioStorageService.download(cert.storageKey)
                val recomputed = MessageDigest.getInstance("SHA-256").digest(pdf)
                recomputed.contentEquals(hashBytes)
            }.getOrDefault(false)

        val valido = signatureOk && artifactOk
        return mapOf(
            "valido" to valido,
            "hashSha256" to cert.hashSha256,
            "chCreditadas" to cert.chCreditadas,
            "issuedAt" to cert.issuedAt,
            "idEvento" to cert.idEvento,
            "origem" to cert.origem,
            "integridadePdf" to artifactOk,
            "verificacaoAssinatura" to if (signatureOk) "ED25519_VALID" else "INVALID",
            "ephemeralKey" to certProperties.ephemeral,
            "_links" to mapOf("jwks" to "/.well-known/jwks.json"),
        )
    }

    private fun verifySignature(
        hashBytes: ByteArray,
        signatureBase64: String,
    ): Boolean {
        if (certProperties.publicKey.isBlank() ||
            signatureBase64.startsWith("UNSIGNED_") ||
            signatureBase64.startsWith("SIGN_ERROR_")
        ) {
            return false
        }
        return try {
            val keyBytes = Base64.getDecoder().decode(certProperties.publicKey)
            val publicKey = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(keyBytes))
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(publicKey)
            sig.update(hashBytes)
            sig.verify(Base64.getDecoder().decode(signatureBase64))
        } catch (_: Exception) {
            false
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.filter { it.isLetterOrDigit() }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
