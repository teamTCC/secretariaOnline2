package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.application.ports.out.PasswordHistoryRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.UsuarioRepository
import br.ufpr.sept.so2.modules.iam.domain.exceptions.WeakPasswordException
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import br.ufpr.sept.so2.shared.audit.AuditPayload
import br.ufpr.sept.so2.shared.audit.AuditPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class FirstAccessCommand(
    val usuarioId: UUID,
    val novaSenha: String,
    val aceiteLgpd: Boolean,
    val ip: String?,
)

@Service
class FirstAccessUseCase(
    private val usuarioRepository: UsuarioRepository,
    private val passwordHistoryRepository: PasswordHistoryRepository,
    private val passwordService: Argon2PasswordService,
    private val auditPublisher: AuditPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: FirstAccessCommand) {
        require(command.aceiteLgpd) { "Aceite da política de privacidade é obrigatório." }

        validatePasswordStrength(command.novaSenha)

        val usuario =
            usuarioRepository.findById(command.usuarioId)
                ?: throw NoSuchElementException("Usuário não encontrado")

        require(usuario.mustChangePassword()) { "Usuário já completou o primeiro acesso." }

        val newHash = passwordService.hash(command.novaSenha)
        usuarioRepository.updatePassword(command.usuarioId, newHash)
        passwordHistoryRepository.save(command.usuarioId, usuario.senhaHash)

        val updatedMetadata =
            usuario.metadata.toMutableMap().apply {
                put("aceite_lgpd_em", OffsetDateTime.now().toString())
            }
        usuarioRepository.updateMetadata(command.usuarioId, updatedMetadata)

        auditPublisher.publish(
            AuditPayload(
                acao = "FIRST_ACCESS_COMPLETED",
                idAtor = command.usuarioId,
                alvoTipo = "usuario",
                alvoId = command.usuarioId,
                ip = command.ip,
                userAgent = null,
                resultado = "OK",
                detalhes = mapOf("aceite_lgpd" to true),
            ),
        )

        log.info("Primeiro acesso concluído para usuario={}", command.usuarioId)
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
