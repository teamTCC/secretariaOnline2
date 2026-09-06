package br.ufpr.sept.so2.modules.estagio.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface InternshipJpaRepository : JpaRepository<InternshipEntity, UUID> {
    fun findAllByIdAluno(
        idAluno: UUID,
        pageable: Pageable,
    ): Page<InternshipEntity>

    fun findAllByIdSupervisor(
        idSupervisor: UUID,
        pageable: Pageable,
    ): Page<InternshipEntity>

    fun findAllByEstado(
        estado: String,
        pageable: Pageable,
    ): Page<InternshipEntity>

    /** COE pool: internships in progress without a supervisor assigned */
    fun findAllByEstadoAndIdSupervisorIsNull(
        estado: String,
        pageable: Pageable,
    ): Page<InternshipEntity>

    fun countByEstado(estado: String): Long

    fun countByIdSupervisorIsNull(): Long

    @Query(
        "SELECT COUNT(i) FROM InternshipEntity i WHERE i.idSupervisor IS NOT NULL AND i.updatedAt >= :after",
    )
    fun countBySupervisorAssignedAfter(
        @Param("after") after: OffsetDateTime,
    ): Long
}

interface InternshipDocumentJpaRepository : JpaRepository<InternshipDocumentEntity, UUID> {
    fun findAllByIdInternship(idInternship: UUID): List<InternshipDocumentEntity>
}
