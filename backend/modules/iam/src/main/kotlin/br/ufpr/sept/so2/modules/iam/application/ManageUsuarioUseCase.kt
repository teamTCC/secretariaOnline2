package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.PasswordResetEnqueuedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioCreatedResponse
import br.ufpr.sept.so2.modules.iam.api.dto.UsuarioStatusResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.RoleJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioRoleEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import br.ufpr.sept.so2.modules.iam.infrastructure.services.JwtTokenService
import br.ufpr.sept.so2.modules.notificacoes.OutboxEventTypes
import br.ufpr.sept.so2.shared.outbox.OutboxEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

data class CreateUsuarioCommand(
    val nome: String,
    val email: String,
    val grr: String?,
    val roleCode: String,
)

@Service
@Transactional
class ManageUsuarioUseCase(
    private val usuarioRepo: UsuarioJpaRepository,
    private val roleRepo: RoleJpaRepository,
    private val argon2PasswordService: Argon2PasswordService,
    private val jwtTokenService: JwtTokenService,
    private val outboxPublisher: OutboxEventPublisher,
) {
    fun create(cmd: CreateUsuarioCommand): UsuarioCreatedResponse {
        require(!usuarioRepo.existsByEmail(cmd.email)) { "Email já cadastrado: ${cmd.email}" }
        cmd.grr?.let { require(!usuarioRepo.existsByGrr(it)) { "GRR já cadastrado: $it" } }

        val role =
            roleRepo
                .findByCode(cmd.roleCode)
                .orElseThrow { NoSuchElementException("Role não encontrada: ${cmd.roleCode}") }

        val senhaTemporaria = UUID.randomUUID().toString().take(12)
        val senhaHash = argon2PasswordService.hash(senhaTemporaria)

        val usuario =
            UsuarioEntity(
                nome = cmd.nome,
                email = cmd.email,
                grr = cmd.grr,
                senhaHash = senhaHash,
                senhaAlterada = false,
            )
        val saved = usuarioRepo.save(usuario)

        val usuarioRole = UsuarioRoleEntity(usuario = saved, role = role)
        saved.usuarioRoles.add(usuarioRole)
        usuarioRepo.save(saved)

        outboxPublisher.enqueue(
            eventType = "iam.usuario_criado",
            aggregateType = "Usuario",
            aggregateId = saved.id,
            payload =
                mapOf(
                    "email" to cmd.email,
                    "nome" to cmd.nome,
                    "senhaTemporaria" to senhaTemporaria,
                ),
        )

        return UsuarioCreatedResponse(id = saved.id, email = saved.email)
    }

    fun updateStatus(
        id: UUID,
        ativo: Boolean,
    ): UsuarioStatusResponse {
        val usuario =
            usuarioRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }
        usuario.ativo = ativo
        usuarioRepo.save(usuario)
        return UsuarioStatusResponse(id = usuario.id, ativo = usuario.ativo)
    }

    fun enqueuePasswordReset(id: UUID): PasswordResetEnqueuedResponse {
        val usuario =
            usuarioRepo
                .findById(id)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $id") }
        require(usuario.ativo) { "Não é possível resetar senha de usuário inativo." }

        val token =
            jwtTokenService.issueOneTimeToken(
                subject = usuario.id,
                audience = "password-reset",
                ttl = Duration.ofHours(24),
            )

        outboxPublisher.enqueue(
            eventType = OutboxEventTypes.PASSWORD_RESET_REQUESTED,
            aggregateType = "Usuario",
            aggregateId = usuario.id,
            payload =
                mapOf(
                    "email" to usuario.email,
                    "nome" to usuario.nome,
                    "token" to token,
                ),
        )

        return PasswordResetEnqueuedResponse(mensagem = "Link de redefinição de senha enviado para ${usuario.email}.")
    }
}
