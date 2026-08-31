package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.EmailOneTimeTokenStore
import br.ufpr.sept.so2.modules.iam.application.ports.out.PasswordHasherPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.PasswordHistoryRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.RefreshTokenRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenRevocationPort
import br.ufpr.sept.so2.modules.iam.application.ports.out.TokenServicePort
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.domain.exceptions.InvalidTokenException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.PasswordReuseException
import br.ufpr.sept.so2.modules.iam.domain.exceptions.WeakPasswordException
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.OffsetDateTime

data class ResetPasswordCommand(
    val token: String,
    val novaSenha: String,
    val ip: String?,
)

@Service
class ResetPasswordUseCase(
    private val tokenService: TokenServicePort,
    private val emailOneTimeTokenStore: EmailOneTimeTokenStore,
    private val usuarioRepository: UsuarioRepository,
    private val passwordHistoryRepository: PasswordHistoryRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenRevocationPort: TokenRevocationPort,
    private val passwordService: PasswordHasherPort,
    private val auditPublisher: AuditPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: ResetPasswordCommand) {
        val parsed =
            try {
                tokenService.parse(command.token)
            } catch (e: Exception) {
                throw InvalidTokenException("Token de redefinição de senha inválido ou expirado.")
            }

        if (parsed.audience.firstOrNull() != "password-reset") {
            throw InvalidTokenException("Token inválido para esta operação.")
        }

        val jti = parsed.jti ?: throw InvalidTokenException("Token malformado.")

        if (emailOneTimeTokenStore.exists(jti)) {
            throw InvalidTokenException("Token de redefinição de senha inválido ou expirado.")
        }

        val usuarioId = parsed.subject
        val usuario =
            usuarioRepository.findById(usuarioId)
                ?: throw InvalidTokenException("Usuário associado ao token não encontrado.")

        validatePasswordStrength(command.novaSenha)

        val recentHashes = passwordHistoryRepository.findRecentHashes(usuarioId, limit = 3)
        val hashesToReject = recentHashes + usuario.senhaHash
        val isReused = hashesToReject.any { hash -> passwordService.verify(command.novaSenha, hash) }
        if (isReused) throw PasswordReuseException()

        val newHash = passwordService.hash(command.novaSenha)

        usuarioRepository.updatePassword(usuarioId, newHash)
        passwordHistoryRepository.save(usuarioId, usuario.senhaHash)

        val expiresAt = parsed.expiresAt
            ?: throw InvalidTokenException("Token malformado.")
        emailOneTimeTokenStore.add(
            jti,
            OffsetDateTime.ofInstant(expiresAt.toInstant(), java.time.ZoneOffset.UTC),
        )

        refreshTokenRepository.revokeAllForUser(usuarioId)
        tokenRevocationPort.forceLogoutUser(
            userId = usuarioId,
            ttl = Duration.ofSeconds(tokenService.accessTtlSeconds + 60),
        )

        auditPublisher.publish(
            AuditPayload(
                acao = "PASSWORD_CHANGED",
                idAtor = usuarioId,
                alvoTipo = "usuario",
                alvoId = usuarioId,
                ip = command.ip,
                userAgent = null,
                resultado = "OK",
            ),
        )

        log.info("Senha redefinida com sucesso para usuario={}", usuarioId)
    }

    private fun validatePasswordStrength(password: String) {
        if (password.length < 12) throw WeakPasswordException("mínimo 12 caracteres")
        if (!password.any { it.isUpperCase() }) throw WeakPasswordException("requer pelo menos uma letra maiúscula")
        if (!password.any { it.isLowerCase() }) throw WeakPasswordException("requer pelo menos uma letra minúscula")
        if (!password.any { it.isDigit() }) throw WeakPasswordException("requer pelo menos um dígito")
        if (!password.any {
                "!@#\$%^&*()_+-=[]{}|;':\",./<>?".contains(
                    it,
                )
            }
        ) {
            throw WeakPasswordException("requer pelo menos um caractere especial")
        }
    }
}
