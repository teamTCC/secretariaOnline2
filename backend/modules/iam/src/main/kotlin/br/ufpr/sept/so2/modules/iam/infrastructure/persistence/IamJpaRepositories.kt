package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
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

    @Query(
        """
        SELECT DISTINCT u FROM UsuarioEntity u
        LEFT JOIN FETCH u.usuarioRoles ur
        LEFT JOIN FETCH ur.role
        WHERE u.id = :id
    """,
    )
    fun findByIdWithRoleAssignments(
        @Param("id") id: UUID,
    ): Optional<UsuarioEntity>

    fun countByAtivoTrue(): Long

    fun countByAtivoTrueAndGrrIsNotNull(): Long

    @Query(
        """
        SELECT COUNT(u) FROM UsuarioEntity u
        JOIN u.usuarioRoles ur JOIN ur.role r
        WHERE r.code = :roleCode
        """,
    )
    fun countByRoleCode(
        @Param("roleCode") roleCode: String,
    ): Long

    @Query(
        """
        SELECT u FROM UsuarioEntity u
        JOIN u.usuarioRoles ur JOIN ur.role r
        WHERE r.code = :roleCode
        """,
    )
    fun findAllByRoleCode(
        @Param("roleCode") roleCode: String,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<UsuarioEntity>

    @Query(
        """
        SELECT u FROM UsuarioEntity u
        WHERE u.ativo = true AND u.grr IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM UsuarioRoleEntity ur JOIN ur.role r
            WHERE ur.usuario = u AND r.code = 'EGRESSO'
        )
        """,
    )
    fun findEligibleForGraduation(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<UsuarioEntity>

    fun existsByEmail(email: String): Boolean

    fun existsByGrr(grr: String): Boolean

    @Query(
        """
        SELECT u FROM UsuarioEntity u
        WHERE LOWER(u.nome) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
        OR (u.grr IS NOT NULL AND u.grr LIKE CONCAT('%', :q, '%'))
        """,
    )
    fun searchByQ(
        @Param("q") q: String,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<UsuarioEntity>

    @Modifying
    @Query("UPDATE UsuarioEntity u SET u.tentativasFalhas = :attempts, u.bloqueadoAte = :bloqueadoAte WHERE u.id = :id")
    fun updateFailedAttempts(
        @Param("id") id: UUID,
        @Param("attempts") attempts: Int,
        @Param("bloqueadoAte") bloqueadoAte: OffsetDateTime?,
    )

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UsuarioEntity u SET u.senhaHash = :hash, u.senhaAlterada = true WHERE u.id = :id")
    fun updatePassword(
        @Param("id") id: UUID,
        @Param("hash") hash: String,
    )

    @Query(
        """
        SELECT u FROM UsuarioEntity u
        WHERE LOWER(u.nome) LIKE CONCAT('%', LOWER(COALESCE(CAST(:nome AS string), '')), '%')
        AND LOWER(u.email) LIKE CONCAT('%', LOWER(COALESCE(CAST(:email AS string), '')), '%')
        AND (:ativo IS NULL OR u.ativo = :ativo)
        """,
    )
    fun searchUsuarios(
        @Param("nome") nome: String?,
        @Param("email") email: String?,
        @Param("ativo") ativo: Boolean?,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<UsuarioEntity>
}

interface RoleJpaRepository : JpaRepository<RoleEntity, UUID> {
    fun findByCode(code: String): Optional<RoleEntity>
}

interface AuthorityJpaRepository : JpaRepository<AuthorityEntity, UUID> {
    fun findByCode(code: String): Optional<AuthorityEntity>

    fun findAllByCodeIn(codes: Collection<String>): List<AuthorityEntity>
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
    @Transactional
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
