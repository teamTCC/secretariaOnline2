package br.ufpr.sept.so2.modules.iam.application.ports.out

import br.ufpr.sept.so2.modules.iam.domain.RefreshToken
import java.util.UUID

interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken

    fun findByValue(value: String): RefreshToken?

    fun markUsed(id: UUID)

    fun revokeAllForUser(usuarioId: UUID)

    fun deleteExpired()
}
