package br.ufpr.sept.so2.modules.auditoria.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface AuditLogJpaRepository : JpaRepository<AuditLogEntity, UUID> {
    @Query(
        """
        SELECT a FROM AuditLogEntity a
        WHERE (:idAtor IS NULL OR a.idAtor = :idAtor)
        AND (:acao IS NULL OR a.acao = :acao)
        AND (:resultado IS NULL OR a.resultado = :resultado)
        AND a.at >= :desde
        ORDER BY a.at DESC
    """,
    )
    fun findWithFilters(
        @Param("idAtor") idAtor: UUID?,
        @Param("acao") acao: String?,
        @Param("resultado") resultado: String?,
        @Param("desde") desde: OffsetDateTime,
        pageable: Pageable,
    ): Page<AuditLogEntity>
}
