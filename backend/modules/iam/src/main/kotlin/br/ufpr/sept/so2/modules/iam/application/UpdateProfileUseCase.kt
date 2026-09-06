package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.arquivos.MinioStorageService
import br.ufpr.sept.so2.modules.iam.api.dto.AvatarUploadUrlResponse
import br.ufpr.sept.so2.modules.iam.api.dto.NotificationPrefsResponse
import br.ufpr.sept.so2.modules.iam.api.dto.ProfileUpdatedResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.NotificationPrefEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.NotificationPrefJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import br.ufpr.sept.so2.modules.iam.infrastructure.services.Argon2PasswordService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UpdateProfileCommand(
    val userId: UUID,
    val nome: String?,
    val metadata: Map<String, Any>?,
)

data class ChangePasswordCommand(
    val userId: UUID,
    val senhaAtual: String,
    val novaSenha: String,
)

data class UpdateNotificationsCommand(
    val userId: UUID,
    val emailEnabled: Boolean?,
    val pushEnabled: Boolean?,
    val inAppEnabled: Boolean?,
)

@Service
@Transactional
class UpdateProfileUseCase(
    private val usuarioJpaRepository: UsuarioJpaRepository,
    private val argon2PasswordService: Argon2PasswordService,
    private val notificationPrefRepo: NotificationPrefJpaRepository,
    private val minioStorageService: MinioStorageService,
) {
    fun updateProfile(cmd: UpdateProfileCommand): ProfileUpdatedResponse {
        val usuario =
            usuarioJpaRepository
                .findById(cmd.userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: ${cmd.userId}") }
        cmd.nome?.let { usuario.nome = it }
        cmd.metadata?.let { usuario.metadata = it.toMutableMap() }
        val saved = usuarioJpaRepository.save(usuario)
        return ProfileUpdatedResponse(id = saved.id, nome = saved.nome)
    }

    fun changePassword(cmd: ChangePasswordCommand) {
        val usuario =
            usuarioJpaRepository
                .findById(cmd.userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: ${cmd.userId}") }
        require(argon2PasswordService.verify(cmd.senhaAtual, usuario.senhaHash)) {
            "Senha atual incorreta."
        }
        val newHash = argon2PasswordService.hash(cmd.novaSenha)
        usuarioJpaRepository.updatePassword(cmd.userId, newHash)
    }

    fun updateNotifications(cmd: UpdateNotificationsCommand): NotificationPrefsResponse {
        val pref =
            notificationPrefRepo.findByIdUsuario(cmd.userId).orElseGet {
                NotificationPrefEntity(idUsuario = cmd.userId)
            }
        cmd.emailEnabled?.let { pref.emailEnabled = it }
        cmd.pushEnabled?.let { pref.pushEnabled = it }
        cmd.inAppEnabled?.let { pref.inAppEnabled = it }
        val saved = notificationPrefRepo.save(pref)
        return NotificationPrefsResponse(
            emailEnabled = saved.emailEnabled,
            pushEnabled = saved.pushEnabled,
            inAppEnabled = saved.inAppEnabled,
        )
    }

    fun requestAvatarUpload(userId: UUID): AvatarUploadUrlResponse {
        val storageKey = "avatars/$userId.jpg"
        val uploadUrl = minioStorageService.generateUploadUrl(storageKey, "image/jpeg", 15)
        return AvatarUploadUrlResponse(uploadUrl = uploadUrl, storageKey = storageKey)
    }
}
