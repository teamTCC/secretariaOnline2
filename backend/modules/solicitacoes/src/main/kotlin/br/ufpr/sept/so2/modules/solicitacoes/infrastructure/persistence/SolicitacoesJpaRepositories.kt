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

    @Query(
        "SELECT r FROM RequestEntity r WHERE LOWER(r.requestTypeCode) LIKE LOWER(CONCAT('%', :q, '%'))",
    )
    fun searchByQ(
        @Param("q") q: String,
        pageable: Pageable,
    ): Page<RequestEntity>

    @Query(
        """
        SELECT r FROM RequestEntity r
        WHERE r.idSolicitante = :idSolicitante
        AND LOWER(r.requestTypeCode) LIKE LOWER(CONCAT('%', :q, '%'))
        """,
    )
    fun searchByQAndSolicitante(
        @Param("q") q: String,
        @Param("idSolicitante") idSolicitante: UUID,
        pageable: Pageable,
    ): Page<RequestEntity>

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

    @Query("SELECT r FROM RequestEntity r WHERE r.numeroAnual = :numeroAnual AND r.ano = :ano")
    fun findByNumeroAnualAndAno(
        @Param("numeroAnual") numeroAnual: Int,
        @Param("ano") ano: Short,
    ): Optional<RequestEntity>

    fun countByEstado(estado: String): Long

    fun countByIdRequestType(idRequestType: UUID): Long

    fun findTop10ByEstadoAndPrazoEmIsNotNullOrderByPrazoEmAsc(estado: String): List<RequestEntity>

    @Query(
        value =
            """
            SELECT request_type_code, COUNT(*)
            FROM request
            WHERE deleted_at IS NULL
              AND (CAST(:cursoId AS uuid) IS NULL OR id_curso = CAST(:cursoId AS uuid))
              AND (CAST(:fromTs AS timestamptz) IS NULL OR created_at >= CAST(:fromTs AS timestamptz))
              AND (CAST(:toTs AS timestamptz) IS NULL OR created_at < CAST(:toTs AS timestamptz))
            GROUP BY request_type_code
            """,
        nativeQuery = true,
    )
    fun countGroupedByTypeFiltered(
        @Param("cursoId") cursoId: UUID?,
        @Param("fromTs") fromTs: java.time.OffsetDateTime?,
        @Param("toTs") toTs: java.time.OffsetDateTime?,
    ): List<Array<Any>>

    @Query(
        value =
            """
            SELECT estado, COUNT(*)
            FROM request
            WHERE deleted_at IS NULL
              AND (CAST(:cursoId AS uuid) IS NULL OR id_curso = CAST(:cursoId AS uuid))
              AND (CAST(:fromTs AS timestamptz) IS NULL OR created_at >= CAST(:fromTs AS timestamptz))
              AND (CAST(:toTs AS timestamptz) IS NULL OR created_at < CAST(:toTs AS timestamptz))
            GROUP BY estado
            """,
        nativeQuery = true,
    )
    fun countGroupedByEstadoFiltered(
        @Param("cursoId") cursoId: UUID?,
        @Param("fromTs") fromTs: java.time.OffsetDateTime?,
        @Param("toTs") toTs: java.time.OffsetDateTime?,
    ): List<Array<Any>>

    @Query(
        value =
            """
            SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS mes, COUNT(*)
            FROM request
            WHERE deleted_at IS NULL
              AND (CAST(:cursoId AS uuid) IS NULL OR id_curso = CAST(:cursoId AS uuid))
              AND (CAST(:fromTs AS timestamptz) IS NULL OR created_at >= CAST(:fromTs AS timestamptz))
              AND (CAST(:toTs AS timestamptz) IS NULL OR created_at < CAST(:toTs AS timestamptz))
            GROUP BY 1 ORDER BY 1
            """,
        nativeQuery = true,
    )
    fun countByMonth(
        @Param("cursoId") cursoId: UUID?,
        @Param("fromTs") fromTs: java.time.OffsetDateTime?,
        @Param("toTs") toTs: java.time.OffsetDateTime?,
    ): List<Array<Any>>

    @Query(
        value =
            """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (concluded_at - created_at))), 0)
            FROM request
            WHERE concluded_at IS NOT NULL
              AND (CAST(:cursoId AS uuid) IS NULL OR id_curso = CAST(:cursoId AS uuid))
            """,
        nativeQuery = true,
    )
    fun avgDeliberationSecondsFiltered(
        @Param("cursoId") cursoId: UUID?,
    ): Number?

    fun countByEstadoAndIdCurso(
        estado: String,
        idCurso: UUID,
    ): Long

    @Query("SELECT r.requestTypeCode, COUNT(r) FROM RequestEntity r GROUP BY r.requestTypeCode")
    fun countGroupedByType(): List<Array<Any>>

    @Query("SELECT r.estado, COUNT(r) FROM RequestEntity r GROUP BY r.estado")
    fun countGroupedByEstado(): List<Array<Any>>

    @Query("SELECT r.idCurso, COUNT(r) FROM RequestEntity r GROUP BY r.idCurso")
    fun countGroupedByCurso(): List<Array<Any>>

    @Query(
        value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (concluded_at - created_at))), 0) FROM request WHERE concluded_at IS NOT NULL",
        nativeQuery = true,
    )
    fun avgDeliberationSeconds(): Number?
}

interface RequestEventJpaRepository : JpaRepository<RequestEventEntity, UUID> {
    fun findAllByIdRequestOrderByCreatedAtAsc(idRequest: UUID): List<RequestEventEntity>

    @Query(
        value =
            """
            SELECT CAST(id_ator AS varchar), COUNT(*)
            FROM request_event
            WHERE estado_novo IN ('DEFERIDA', 'INDEFERIDA', 'DELIBERADA')
            GROUP BY id_ator
            ORDER BY COUNT(*) DESC
            """,
        nativeQuery = true,
    )
    fun countCargaPorDeliberador(): List<Array<Any>>
}

interface RequestAttachmentJpaRepository : JpaRepository<RequestAttachmentEntity, UUID> {
    fun findAllByIdRequest(idRequest: UUID): List<RequestAttachmentEntity>
}

interface RequestTypeVersionJpaRepository : JpaRepository<RequestTypeVersionEntity, UUID> {
    fun findFirstByIdRequestTypeOrderByVersionDesc(idRequestType: UUID): Optional<RequestTypeVersionEntity>

    @Query("SELECT MAX(v.version) FROM RequestTypeVersionEntity v WHERE v.idRequestType = :idRequestType")
    fun findMaxVersion(
        @Param("idRequestType") idRequestType: UUID,
    ): Int?
}
