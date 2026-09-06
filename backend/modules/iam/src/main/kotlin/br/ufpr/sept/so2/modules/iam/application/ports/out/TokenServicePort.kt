package br.ufpr.sept.so2.modules.iam.application.ports.out

import br.ufpr.sept.so2.modules.iam.domain.Usuario
import java.time.Duration
import java.util.Date
import java.util.UUID

data class ParsedToken(
    val subject: UUID,
    val jti: String?,
    val audience: Set<String>,
    val expiresAt: Date?,
    val issuedAt: Date?,
)

interface TokenServicePort {
    val accessTtlSeconds: Long

    fun issueAccessToken(
        usuario: Usuario,
        sid: String,
    ): String

    fun issueOneTimeToken(
        subject: UUID,
        audience: String,
        ttl: Duration,
    ): String

    fun parse(token: String): ParsedToken
}

interface PasswordHasherPort {
    fun hash(rawPassword: String): String

    fun verify(
        rawPassword: String,
        hash: String,
    ): Boolean
}
