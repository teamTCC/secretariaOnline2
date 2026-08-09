package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.util.UUID

interface PasswordHistoryRepository {
    fun save(
        usuarioId: UUID,
        hash: String,
    )

    fun findRecentHashes(
        usuarioId: UUID,
        limit: Int,
    ): List<String>
}
