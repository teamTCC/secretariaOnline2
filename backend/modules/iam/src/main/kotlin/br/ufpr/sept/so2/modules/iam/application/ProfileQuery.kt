package br.ufpr.sept.so2.modules.iam.application

import br.ufpr.sept.so2.modules.iam.api.dto.ProfileResponse
import br.ufpr.sept.so2.modules.iam.infrastructure.persistence.UsuarioJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ProfileQuery(
    private val usuarioJpaRepository: UsuarioJpaRepository,
) {
    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): ProfileResponse {
        val usuario =
            usuarioJpaRepository
                .findByIdWithRoleAssignments(userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $userId") }
        val roles = usuario.usuarioRoles.map { it.role.code }.distinct()
        return ProfileResponse(
            id = usuario.id,
            nome = usuario.nome,
            email = usuario.email,
            grr = usuario.grr,
            ativo = usuario.ativo,
            metadata = usuario.metadata,
            roles = roles,
            links =
                mapOf(
                    "self" to "/me",
                    "update-profile" to "/me",
                    "change-password" to "/me/password",
                    "notifications" to "/me/notifications",
                    "data-export" to "/me/data-export",
                ),
        )
    }
}
