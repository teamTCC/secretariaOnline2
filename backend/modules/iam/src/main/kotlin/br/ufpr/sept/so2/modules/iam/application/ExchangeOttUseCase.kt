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

data class ExchangeOttCommand(
    val token: String,
    val ip: String?,
    val userAgent: String?,
)

@Service
class ExchangeOttUseCase(
    private val tokenService: TokenServicePort,
    private val usuarioRepository: UsuarioRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenRevocationPort: TokenRevocationPort,
    private val auditPublisher: AuditPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: ExchangeOttCommand): LoginResult {
        val parsed =
            try {
                tokenService.parse(command.token)
            } catch (_: Exception) {
                throw InvalidTokenException("Token one-time inválido ou expirado.")
            }

        if (parsed.audience.none { it.startsWith("request:") }) {
            throw InvalidTokenException("Token inválido para esta operação.")
        }

        val jti = parsed.jti ?: throw InvalidTokenException("Token malformado.")
        if (tokenRevocationPort.isRevoked(jti)) {
            throw InvalidTokenException("Token já utilizado. Solicite um novo link.")
        }

        val usuario =
            usuarioRepository.findById(parsed.subject)
                ?: throw InvalidTokenException("Usuário associado ao token não encontrado.")
        if (!usuario.ativo) {
            throw InvalidTokenException("Usuário inativo.")
        }

        val expiresAt = parsed.expiresAt ?: throw InvalidTokenException("Token malformado.")
        tokenRevocationPort.revokeAccessToken(jti, expiresAt)

        val sid = UUID.randomUUID().toString()
        val accessToken = tokenService.issueAccessToken(usuario, sid)
        val sessionTtl = Duration.ofSeconds(tokenService.accessTtlSeconds + 60)
        tokenRevocationPort.createSession(sid, usuario.id, sessionTtl)

        val refreshToken = RefreshToken.issue(usuario.id)
        refreshTokenRepository.save(refreshToken)

        auditPublisher.publish(
            AuditPayload(
                acao = "OTT_EXCHANGED",
                idAtor = usuario.id,
                alvoTipo = "usuario",
                alvoId = usuario.id,
                ip = command.ip,
                userAgent = command.userAgent,
                resultado = "OK",
                detalhes = mapOf("audience" to parsed.audience.joinToString()),
            ),
        )
        log.info("OTT trocado por sessão para usuario={}", usuario.id)

        return LoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken.value,
            mustChangePassword = usuario.mustChangePassword(),
            mustAcceptLgpd = !usuario.aceitouLgpd(),
        )
    }
}
