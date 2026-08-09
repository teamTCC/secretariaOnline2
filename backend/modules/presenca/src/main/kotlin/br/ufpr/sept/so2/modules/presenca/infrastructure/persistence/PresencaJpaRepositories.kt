package br.ufpr.sept.so2.modules.presenca.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

interface EventAttendanceJpaRepository : JpaRepository<EventAttendanceEntity, UUID> {
    fun findAllByIdOrganizador(
        idOrganizador: UUID,
        pageable: Pageable,
    ): Page<EventAttendanceEntity>

    @Query(
        """
        SELECT e FROM EventAttendanceEntity e
        WHERE (:estado IS NULL OR e.estado = :estado)
        AND (:idOrganizador IS NULL OR e.idOrganizador = :idOrganizador)
        AND (:idCurso IS NULL OR e.idCurso = :idCurso)
    """,
    )
    fun findWithFilters(
        @Param("estado") estado: String?,
        @Param("idOrganizador") idOrganizador: UUID?,
        @Param("idCurso") idCurso: UUID?,
        pageable: Pageable,
    ): Page<EventAttendanceEntity>

    @Modifying
    @Query("UPDATE EventAttendanceEntity e SET e.estado = :estado WHERE e.id = :id")
    fun updateEstado(
        @Param("id") id: UUID,
        @Param("estado") estado: String,
    )

    @Query("SELECT e FROM EventAttendanceEntity e WHERE e.estado = 'EM_ANDAMENTO' AND e.fimEm < :now")
    fun findOverdueInProgress(
        @Param("now") now: OffsetDateTime,
    ): List<EventAttendanceEntity>
}

interface AttendanceSessionJpaRepository : JpaRepository<AttendanceSessionEntity, UUID> {
    fun findByIdEventoAndIdAluno(
        idEvento: UUID,
        idAluno: UUID,
    ): Optional<AttendanceSessionEntity>

    fun findAllByIdEvento(idEvento: UUID): List<AttendanceSessionEntity>

    fun countByIdEventoAndEntryConfirmedAtIsNotNull(idEvento: UUID): Long

    @Modifying
    @Query("UPDATE AttendanceSessionEntity s SET s.entryConfirmedAt = :now WHERE s.id = :id")
    fun confirmEntry(
        @Param("id") id: UUID,
        @Param("now") now: OffsetDateTime,
    )

    @Modifying
    @Query("UPDATE AttendanceSessionEntity s SET s.exitConfirmedAt = :now WHERE s.id = :id")
    fun confirmExit(
        @Param("id") id: UUID,
        @Param("now") now: OffsetDateTime,
    )

    fun existsByIdEventoAndDeviceUuid(
        idEvento: UUID,
        deviceUuid: String,
    ): Boolean
}

interface CertificateJpaRepository : JpaRepository<CertificateEntity, UUID> {
    fun findByHashSha256(hash: String): Optional<CertificateEntity>

    fun findAllByIdAluno(idAluno: UUID): List<CertificateEntity>

    fun findByIdEventoAndIdAluno(
        idEvento: UUID,
        idAluno: UUID,
    ): Optional<CertificateEntity>
}
