package br.ufpr.sept.so2.modules.iam.domain

import br.ufpr.sept.so2.shared.domain.AggregateRoot
import br.ufpr.sept.so2.shared.domain.valueobject.Email
import br.ufpr.sept.so2.shared.domain.valueobject.Grr
import java.time.OffsetDateTime
import java.util.UUID

data class Usuario(
    val id: UUID,
    val nome: String,
    val email: Email,
    val grr: Grr?,
    val senhaHash: String,
    val senhaAlterada: Boolean,
    val ativo: Boolean,
    val bloqueadoAte: OffsetDateTime?,
    val tentativasFalhas: Int,
    val metadata: Map<String, Any>,
    val roles: Set<UsuarioRole>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) : AggregateRoot() {
    fun verificaSenha(
        rawPassword: String,
        encoder: PasswordEncoder,
    ): Boolean = encoder.matches(rawPassword, senhaHash)

    fun estaBloqueado(): Boolean = bloqueadoAte?.isAfter(OffsetDateTime.now()) == true

    fun estaAtivo(): Boolean = ativo && !estaBloqueado()

    fun authorities(): Set<String> = roles.flatMap { it.role.authorities }.map { it.code }.toSet()

    fun mustChangePassword(): Boolean = !senhaAlterada

    fun aceitouLgpd(): Boolean = metadata.containsKey("aceite_lgpd_em")

    companion object {
        const val MAX_FAILED_ATTEMPTS = 10
        val LOCK_DURATION_MINUTES = 15L
    }
}

data class UsuarioRole(
    val role: Role,
    val escopo: Map<String, Any> = emptyMap(),
)

fun interface PasswordEncoder {
    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean
}
