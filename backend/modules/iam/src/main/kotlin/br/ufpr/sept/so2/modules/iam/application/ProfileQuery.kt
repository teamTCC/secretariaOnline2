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
                .findByIdWithRoles(userId)
                .orElseThrow { NoSuchElementException("Usuário não encontrado: $userId") }
        val roles = usuario.usuarioRoles.map { it.role.code }.distinct()
        val mustChangePassword = !usuario.senhaAlterada
        val mustAcceptLgpd = !usuario.metadata.containsKey("aceite_lgpd_em")
        val links =
            linkedMapOf(
                "self" to "/me",
                "update-profile" to "/me",
                "change-password" to "/me/password",
                "notifications" to "/me/notifications",
                "data-export" to "/me/data-export",
            )
        if (roles.any { it.equals("ALUNO", ignoreCase = true) }) {
            links["dashboard"] = "/bff/dashboard/aluno"
        }
        return ProfileResponse(
            id = usuario.id,
            nome = usuario.nome,
            email = usuario.email,
            grr = usuario.grr,
            ativo = usuario.ativo,
            metadata = usuario.metadata,
            roles = roles,
            mustChangePassword = mustChangePassword,
            mustAcceptLgpd = mustAcceptLgpd,
            links = links,
        )
    }
}
