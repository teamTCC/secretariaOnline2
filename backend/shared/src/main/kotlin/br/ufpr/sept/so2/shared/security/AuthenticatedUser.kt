package br.ufpr.sept.so2.shared.security

import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

data class AuthenticatedUser(
    val userId: UUID,
    val authorities: Set<String>,
)

fun currentUser(): AuthenticatedUser {
    val auth =
        SecurityContextHolder.getContext().authentication
            ?: error("Nenhum usuário autenticado no contexto de segurança")

    val principal =
        auth.principal as? AuthenticatedUser
            ?: error("Principal não é do tipo AuthenticatedUser")

    return principal
}

fun currentUserId(): UUID = currentUser().userId

fun hasAuthority(authority: String): Boolean = currentUser().authorities.contains(authority)
