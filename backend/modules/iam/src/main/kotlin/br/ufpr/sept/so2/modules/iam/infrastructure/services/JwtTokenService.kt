package br.ufpr.sept.so2.modules.iam.infrastructure.services

import br.ufpr.sept.so2.modules.iam.application.ports.out.ParsedToken
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenServicePort
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
    @Value("\${security.jwt.access-token-ttl-seconds:900}") override val accessTtlSeconds: Long,
    @Value("\${security.jwt.issuer:secretaria-online-2}") private val issuer: String,
) : TokenServicePort {
    private val privateKey: RSAPrivateKey by lazy { parsePrivateKey(privateKeyPem) }
    val publicKey: RSAPublicKey by lazy { parsePublicKey(publicKeyPem) }

    /**
     * Issues an RS256 access token bound to [sid] (session ID).
     *
     * The [sid] claim links this token to a Redis session entry (`auth:session:<sid>`).
     * The [JwtAuthenticationFilter] verifies the session exists in Redis on every request
     * (fail-closed). Deleting the Redis key instantly invalidates any access token that
     * carries this [sid], regardless of JWT expiry — enabling true instantaneous logout.
     */
    override fun issueAccessToken(usuario: Usuario, sid: String): String =
        Jwts
            .builder()
            .id(UUID.randomUUID().toString())
            .issuer(issuer)
            .subject(usuario.id.toString())
            .claim("sid", sid)
            .claim("authorities", usuario.authorities().toList())
            .claim("nome", usuario.nome)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessTtlSeconds * 1000))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()

    override fun issueOneTimeToken(
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

    override fun parse(token: String): ParsedToken {
        val payload = verify(token).payload
        return ParsedToken(
            subject = UUID.fromString(payload.subject),
            jti = payload.id,
            audience = payload.audience ?: emptySet(),
            expiresAt = payload.expiration,
            issuedAt = payload.issuedAt,
        )
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
