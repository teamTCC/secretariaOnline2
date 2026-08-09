package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

interface UsuarioJpaRepository : JpaRepository<UsuarioEntity, UUID> {
    @Query(
        """
        SELECT u FROM UsuarioEntity u
        LEFT JOIN FETCH u.usuarioRoles ur
        LEFT JOIN FETCH ur.role r
        LEFT JOIN FETCH r.authorities
        WHERE LOWER(u.email) = LOWER(:email)
    """,
    )
    fun findByEmailWithRoles(
        @Param("email") email: String,
    ): Optional<UsuarioEntity>

    @Query(
        """
        SELECT u FROM UsuarioEntity u
        LEFT JOIN FETCH u.usuarioRoles ur
        LEFT JOIN FETCH ur.role r
        LEFT JOIN FETCH r.authorities
        WHERE u.grr = :grr
    """,
    )
    fun findByGrrWithRoles(
        @Param("grr") grr: String,
    ): Optional<UsuarioEntity>

    @Query(
        """
        SELECT u FROM UsuarioEntity u
        LEFT JOIN FETCH u.usuarioRoles ur
        LEFT JOIN FETCH ur.role r
        LEFT JOIN FETCH r.authorities
        WHERE u.id = :id
    """,
    )
    fun findByIdWithRoles(
        @Param("id") id: UUID,
    ): Optional<UsuarioEntity>

    fun existsByEmail(email: String): Boolean

    fun existsByGrr(grr: String): Boolean

    @Modifying
    @Query("UPDATE UsuarioEntity u SET u.tentativasFalhas = :attempts, u.bloqueadoAte = :bloqueadoAte WHERE u.id = :id")
    fun updateFailedAttempts(
        @Param("id") id: UUID,
        @Param("attempts") attempts: Int,
        @Param("bloqueadoAte") bloqueadoAte: OffsetDateTime?,
    )

    @Modifying
    @Query("UPDATE UsuarioEntity u SET u.senhaHash = :hash, u.senhaAlterada = true WHERE u.id = :id")
    fun updatePassword(
        @Param("id") id: UUID,
        @Param("hash") hash: String,
    )
}

interface RoleJpaRepository : JpaRepository<RoleEntity, UUID> {
    fun findByCode(code: String): Optional<RoleEntity>
}

interface AuthorityJpaRepository : JpaRepository<AuthorityEntity, UUID> {
    fun findByCode(code: String): Optional<AuthorityEntity>
}

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByValue(value: String): Optional<RefreshTokenEntity>

    @Modifying
    @Query("UPDATE RefreshTokenEntity t SET t.usedAt = :now WHERE t.id = :id")
    fun markUsed(
        @Param("id") id: UUID,
        @Param("now") now: OffsetDateTime,
    )

    @Modifying
    @Query("UPDATE RefreshTokenEntity t SET t.revokedAt = :now WHERE t.usuarioId = :usuarioId AND t.revokedAt IS NULL")
    fun revokeAllForUser(
        @Param("usuarioId") usuarioId: UUID,
        @Param("now") now: OffsetDateTime,
    )

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :now")
    fun deleteExpired(
        @Param("now") now: OffsetDateTime,
    )
}

interface JtiBlacklistJpaRepository : JpaRepository<JtiBlacklistEntity, String> {
    fun existsByJti(jti: String): Boolean

    @Modifying
    @Query("DELETE FROM JtiBlacklistEntity j WHERE j.expiresAt < :now")
    fun deleteExpired(
        @Param("now") now: OffsetDateTime,
    )
}

interface PasswordHistoryJpaRepository : JpaRepository<PasswordHistoryEntity, UUID> {
    @Query("SELECT p FROM PasswordHistoryEntity p WHERE p.usuarioId = :usuarioId ORDER BY p.createdAt DESC")
    fun findRecentByUsuario(
        @Param("usuarioId") usuarioId: UUID,
        pageable: org.springframework.data.domain.Pageable,
    ): List<PasswordHistoryEntity>
}
