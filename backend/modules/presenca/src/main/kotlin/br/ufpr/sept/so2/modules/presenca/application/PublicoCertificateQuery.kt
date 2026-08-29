package br.ufpr.sept.so2.modules.presenca.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.presenca.api.dto.CertificateVerificationResponse
import br.ufpr.sept.so2.modules.presenca.config.CertificateProperties
import br.ufpr.sept.so2.modules.presenca.infrastructure.persistence.CertificateJpaRepository
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Component
class PublicoCertificateQuery(
    private val certificateRepo: CertificateJpaRepository,
    private val certProperties: CertificateProperties,
    private val minioStorageService: MinioStorageService,
) {
    fun verificarCertificado(hash: String): CertificateVerificationResponse {
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
        return CertificateVerificationResponse(
            valido = valido,
            hashSha256 = cert.hashSha256,
            chCreditadas = cert.chCreditadas,
            issuedAt = cert.issuedAt,
            idEvento = cert.idEvento,
            origem = cert.origem,
            integridadePdf = artifactOk,
            verificacaoAssinatura = if (signatureOk) "ED25519_VALID" else "INVALID",
            ephemeralKey = certProperties.ephemeral,
            links = mapOf("jwks" to "/.well-known/jwks.json"),
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
