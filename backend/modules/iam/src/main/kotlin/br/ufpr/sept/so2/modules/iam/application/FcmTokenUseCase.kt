package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FcmTokenEntity
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.FcmTokenJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class FcmTokenUseCase(
    private val fcmTokenRepo: FcmTokenJpaRepository,
) {
    fun register(idUsuario: UUID, fcmToken: String, plataforma: String) {
        val existing = fcmTokenRepo.findByIdUsuarioAndFcmToken(idUsuario, fcmToken)
        if (existing.isPresent) {
            val token = existing.get()
            token.ativo = true
            token.plataforma = plataforma
            fcmTokenRepo.save(token)
        } else {
            fcmTokenRepo.save(
                FcmTokenEntity(
                    idUsuario = idUsuario,
                    fcmToken = fcmToken,
                    plataforma = plataforma,
                    ativo = true,
                ),
            )
        }
    }

    fun unregister(idUsuario: UUID, fcmToken: String) {
        fcmTokenRepo.findByIdUsuarioAndFcmToken(idUsuario, fcmToken).ifPresent { token ->
            token.ativo = false
            fcmTokenRepo.save(token)
        }
    }
}
