package br.ufpr.sept.so2.modules.presenca.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.certificate")
class CertificateProperties {
    /** Ed25519 PKCS8 private key base64-encoded. Used to sign certificates. */
    var privateKey: String = ""

    /** Ed25519 X509 public key base64-encoded. Exposed at /.well-known/jwks.json */
    var publicKey: String = ""

    /** True when keys were generated at startup because env vars were blank. */
    var ephemeral: Boolean = false
}
