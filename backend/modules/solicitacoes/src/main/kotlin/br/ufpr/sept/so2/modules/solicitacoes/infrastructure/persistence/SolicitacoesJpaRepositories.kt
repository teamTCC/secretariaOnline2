package br.ufpr.sept.so2.modules.solicitacoes.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface RequestTypeJpaRepository : JpaRepository<RequestTypeEntity, UUID> {
    fun findByCode(code: String): Optional<RequestTypeEntity>

    fun findAllByAtivoTrue(): List<RequestTypeEntity>
}

interface RequestJpaRepository : JpaRepository<RequestEntity, UUID> {
    fun findAllByIdSolicitante(
        idSolicitante: UUID,
        pageable: Pageable,
    ): Page<RequestEntity>

    fun findAllByIdCurso(
        idCurso: UUID,
        pageable: Pageable,
    ): Page<RequestEntity>

    @Query(
        """
        SELECT r FROM RequestEntity r
        WHERE (:estado IS NULL OR r.estado = :estado)
        AND (:idSolicitante IS NULL OR r.idSolicitante = :idSolicitante)
        AND (:idCurso IS NULL OR r.idCurso = :idCurso)
        AND (:typeCode IS NULL OR r.requestTypeCode = :typeCode)
    """,
    )
    fun findWithFilters(
        @Param("estado") estado: String?,
        @Param("idSolicitante") idSolicitante: UUID?,
        @Param("idCurso") idCurso: UUID?,
        @Param("typeCode") typeCode: String?,
        pageable: Pageable,
    ): Page<RequestEntity>

    @Query("SELECT MAX(r.numeroAnual) FROM RequestEntity r WHERE r.ano = :ano AND r.idCurso = :idCurso")
    fun findMaxNumeroAnual(
        @Param("ano") ano: Short,
        @Param("idCurso") idCurso: UUID,
    ): Int?

    @Modifying
    @Query(
        "UPDATE RequestEntity r SET r.estado = :estado, r.parecer = :parecer, r.concludedAt = CASE WHEN :concluded = true THEN CURRENT_TIMESTAMP ELSE r.concludedAt END WHERE r.id = :id",
    )
    fun updateEstado(
        @Param("id") id: UUID,
        @Param("estado") estado: String,
        @Param("parecer") parecer: String?,
        @Param("concluded") concluded: Boolean,
    )
}

interface RequestEventJpaRepository : JpaRepository<RequestEventEntity, UUID> {
    fun findAllByIdRequestOrderByCreatedAtAsc(idRequest: UUID): List<RequestEventEntity>
}

interface RequestAttachmentJpaRepository : JpaRepository<RequestAttachmentEntity, UUID> {
    fun findAllByIdRequest(idRequest: UUID): List<RequestAttachmentEntity>
}
