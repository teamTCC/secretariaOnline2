package br.ufpr.sept.so2.modules.formativas.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface FormativeActivityJpaRepository : JpaRepository<FormativeActivityEntity, UUID> {
    fun findAllByIdAluno(
        idAluno: UUID,
        pageable: Pageable,
    ): Page<FormativeActivityEntity>

    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<FormativeActivityEntity>

    /** CAAF pool: pendente without a reviewer assigned yet */
    fun findAllByEstadoAndIdRevisorIsNull(
        estado: String,
        pageable: Pageable,
    ): Page<FormativeActivityEntity>

    fun countByEstado(estado: String): Long

    @Query(
        "SELECT a.categoria, COUNT(a) FROM FormativeActivityEntity a WHERE a.estado = 'APROVADA' GROUP BY a.categoria",
    )
    fun countAprovadasByCategoria(): List<Array<Any>>

    @Query(
        "SELECT COUNT(a) FROM FormativeActivityEntity a WHERE a.estado = :estado AND a.updatedAt >= :after",
    )
    fun countByEstadoAndUpdatedAtAfter(
        @Param("estado") estado: String,
        @Param("after") after: OffsetDateTime,
    ): Long
}

interface FormativeEntryJpaRepository : JpaRepository<FormativeEntryEntity, UUID> {
    fun findAllByIdAluno(idAluno: UUID): List<FormativeEntryEntity>

    @Query("SELECT COALESCE(SUM(e.horasAprovadas), 0) FROM FormativeEntryEntity e WHERE e.idAluno = :alunoId")
    fun sumHorasAprovadas(
        @Param("alunoId") alunoId: UUID,
    ): Double

    fun existsByIdActivity(idActivity: UUID): Boolean
}
