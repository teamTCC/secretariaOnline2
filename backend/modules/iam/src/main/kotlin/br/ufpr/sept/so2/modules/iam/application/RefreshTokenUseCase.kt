package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.RefreshTokenRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenRevocationPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenServicePort
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.domain.RefreshToken
import br.ufpr.sept.so2.modules.iam.domain.exceptions.InvalidTokenException
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

data class RefreshTokenCommand(
    val refreshTokenValue: String,
    val ip: String?,
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Service
class RefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val usuarioRepository: UsuarioRepository,
    private val tokenService: TokenServicePort,
    private val auditPublisher: AuditPublisher,
    private val tokenRevocationPort: TokenRevocationPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: RefreshTokenCommand): TokenPair {
        val stored =
            refreshTokenRepository.findByValue(command.refreshTokenValue)
                ?: throw InvalidTokenException("Refresh token inválido")

        if (stored.isExpired()) {
            throw InvalidTokenException("Refresh token expirado. Faça login novamente.")
        }

        if (stored.isUsed() || stored.isRevoked()) {
            // Reuse detection: revoke ALL sessions for this user (possible token theft).
            // Also mark the user as force-logged-out in Redis so any live access token
            // that was issued before this moment is immediately rejected.
            refreshTokenRepository.revokeAllForUser(stored.usuarioId)
            tokenRevocationPort.forceLogoutUser(
                userId = stored.usuarioId,
                ttl = Duration.ofSeconds(tokenService.accessTtlSeconds),
            )
            auditPublisher.publish(
                AuditPayload(
                    acao = "SUSPICIOUS_TOKEN_REUSE",
                    idAtor = stored.usuarioId,
                    alvoTipo = "usuario",
                    alvoId = stored.usuarioId,
                    ip = command.ip,
                    userAgent = null,
                    resultado = "DENIED",
                    detalhes = mapOf("razao" to "TOKEN_REUTILIZADO"),
                ),
            )
            log.warn("Reutilização suspeita de refresh token para usuario={}", stored.usuarioId)
            throw InvalidTokenException("Token já utilizado — todas as sessões foram encerradas por segurança.")
        }

        val usuario =
            usuarioRepository.findById(stored.usuarioId)
                ?: throw InvalidTokenException("Usuário não encontrado")

        refreshTokenRepository.markUsed(stored.id)

        val newRefreshToken = RefreshToken.issue(usuario.id)
        refreshTokenRepository.save(newRefreshToken)

        val sid = UUID.randomUUID().toString()
        val sessionTtl = Duration.ofSeconds(tokenService.accessTtlSeconds + 60)
        tokenRevocationPort.createSession(sid, usuario.id, sessionTtl)
        val newAccessToken = tokenService.issueAccessToken(usuario, sid)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken.value,
        )
    }
}
