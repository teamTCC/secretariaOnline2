package br.ufpr.sept.so2.modules.iam.infrastructure.services

import br.ufpr.sept.so2.modules.iam.domain.Usuario
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class JwtTokenService(
    @Value("\${security.jwt.private-key}") private val privateKeyPem: String,
    @Value("\${security.jwt.public-key}") private val publicKeyPem: String,
    @Value("\${security.jwt.access-token-ttl-seconds:900}") private val accessTtlSeconds: Long,
    @Value("\${security.jwt.issuer:secretaria-online-2}") private val issuer: String,
) {
    private val privateKey: RSAPrivateKey by lazy { parsePrivateKey(privateKeyPem) }
    val publicKey: RSAPublicKey by lazy { parsePublicKey(publicKeyPem) }

    fun issueAccessToken(usuario: Usuario): String =
        Jwts
            .builder()
            .id(UUID.randomUUID().toString())
            .issuer(issuer)
            .subject(usuario.id.toString())
            .claim("authorities", usuario.authorities().toList())
            .claim("nome", usuario.nome)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessTtlSeconds * 1000))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()

    fun issueOneTimeToken(
        subject: UUID,
        audience: String,
        ttl: Duration,
    ): String {
        val jti = UUID.randomUUID().toString()
        return Jwts
            .builder()
            .issuer(issuer)
            .id(jti)
            .subject(subject.toString())
            .audience()
            .add(audience)
            .and()
            .expiration(Date(System.currentTimeMillis() + ttl.toMillis()))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()
    }

    fun verify(token: String): Jws<Claims> =
        Jwts
            .parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)

    @Suppress("UNCHECKED_CAST")
    fun extractAuthorities(token: String): Set<String> {
        val claims = verify(token).payload
        return (claims["authorities"] as? List<String>)?.toSet() ?: emptySet()
    }

    fun extractUserId(token: String): UUID = UUID.fromString(verify(token).payload.subject)

    private fun parsePrivateKey(pem: String): RSAPrivateKey {
        val cleaned =
            pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim()
        val keyBytes = Base64.getDecoder().decode(cleaned)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as RSAPrivateKey
    }

    private fun parsePublicKey(pem: String): RSAPublicKey {
        val cleaned =
            pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim()
        val keyBytes = Base64.getDecoder().decode(cleaned)
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes)) as RSAPublicKey
    }
}
