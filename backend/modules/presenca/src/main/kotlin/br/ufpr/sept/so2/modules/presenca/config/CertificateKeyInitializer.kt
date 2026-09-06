package br.ufpr.sept.so2.modules.presenca.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.KeyPairGenerator
import java.util.Base64

/**
 * Em dev, se CERT_PRIVATE_KEY/CERT_PUBLIC_KEY estiverem vazios, gera um par Ed25519
 * efêmero na subida — certificados nunca saem com prefixo UNSIGNED_.
 */
@Component
class CertificateKeyInitializer(
    private val props: CertificateProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun ensureKeys() {
        if (props.privateKey.isNotBlank() && props.publicKey.isNotBlank()) {
            return
        }
        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        props.privateKey = Base64.getEncoder().encodeToString(kp.private.encoded)
        props.publicKey = Base64.getEncoder().encodeToString(kp.public.encoded)
        props.ephemeral = true
        log.warn(
            "Chaves Ed25519 de certificado não configuradas — gerado par efêmero em memória. " +
                "Defina CERT_PRIVATE_KEY / CERT_PUBLIC_KEY em produção.",
        )
    }
}
