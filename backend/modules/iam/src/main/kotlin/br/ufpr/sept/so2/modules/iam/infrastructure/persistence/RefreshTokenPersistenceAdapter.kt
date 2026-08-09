package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import br.ufpr.sept.so2.modules.iam.application.ports.out.JtiBlacklistRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.PasswordHistoryRepository
import br.ufpr.sept.so2.modules.iam.application.ports.out.RefreshTokenRepository
import br.ufpr.sept.so2.modules.iam.domain.RefreshToken
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class RefreshTokenPersistenceAdapter(
    private val jpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenRepository {
    override fun save(token: RefreshToken): RefreshToken {
        val entity =
            RefreshTokenEntity(
                id = token.id,
                value = token.value,
                usuarioId = token.usuarioId,
                expiresAt = token.expiresAt,
                usedAt = token.usedAt,
                revokedAt = token.revokedAt,
            )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findByValue(value: String): RefreshToken? = jpaRepository.findByValue(value).map { it.toDomain() }.orElse(null)

    override fun markUsed(id: UUID) {
        jpaRepository.markUsed(id, OffsetDateTime.now())
    }

    override fun revokeAllForUser(usuarioId: UUID) {
        jpaRepository.revokeAllForUser(usuarioId, OffsetDateTime.now())
    }

    override fun deleteExpired() {
        jpaRepository.deleteExpired(OffsetDateTime.now())
    }

    private fun RefreshTokenEntity.toDomain(): RefreshToken =
        RefreshToken(
            id = this.id,
            value = this.value,
            usuarioId = this.usuarioId,
            expiresAt = this.expiresAt,
            usedAt = this.usedAt,
            revokedAt = this.revokedAt,
            createdAt = this.createdAt,
        )
}

@Repository
class JtiBlacklistPersistenceAdapter(
    private val jpaRepository: JtiBlacklistJpaRepository,
) : JtiBlacklistRepository {
    override fun add(
        jti: String,
        expiresAt: OffsetDateTime,
    ) {
        jpaRepository.save(JtiBlacklistEntity(jti = jti, expiresAt = expiresAt))
    }

    override fun exists(jti: String): Boolean = jpaRepository.existsByJti(jti)

    override fun deleteExpired() {
        jpaRepository.deleteExpired(OffsetDateTime.now())
    }
}

@Repository
class PasswordHistoryPersistenceAdapter(
    private val jpaRepository: PasswordHistoryJpaRepository,
) : PasswordHistoryRepository {
    override fun save(
        usuarioId: UUID,
        hash: String,
    ) {
        jpaRepository.save(PasswordHistoryEntity(usuarioId = usuarioId, senhaHash = hash))
    }

    override fun findRecentHashes(
        usuarioId: UUID,
        limit: Int,
    ): List<String> = jpaRepository.findRecentByUsuario(usuarioId, PageRequest.of(0, limit)).map { it.senhaHash }
}
