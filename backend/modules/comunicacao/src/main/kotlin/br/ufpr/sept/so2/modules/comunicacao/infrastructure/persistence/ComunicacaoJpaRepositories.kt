package br.ufpr.sept.so2.modules.comunicacao.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

interface CommunicationJpaRepository : JpaRepository<CommunicationEntity, UUID> {
    fun findAllByPublishedAtIsNotNullOrderByPublishedAtDesc(pageable: Pageable): Page<CommunicationEntity>

    fun findAllByIdAutor(
        idAutor: UUID,
        pageable: Pageable,
    ): Page<CommunicationEntity>
}

interface CommunicationDeliveryJpaRepository : JpaRepository<CommunicationDeliveryEntity, UUID> {
    fun findAllByIdUsuarioOrderByDeliveredAtDesc(
        idUsuario: UUID,
        pageable: Pageable,
    ): Page<CommunicationDeliveryEntity>

    fun findByIdCommunicationAndIdUsuario(
        idCommunication: UUID,
        idUsuario: UUID,
    ): Optional<CommunicationDeliveryEntity>

    @Modifying
    @Query("UPDATE CommunicationDeliveryEntity d SET d.readAt = :now WHERE d.id = :id AND d.readAt IS NULL")
    fun markRead(
        @Param("id") id: UUID,
        @Param("now") now: OffsetDateTime,
    )

    fun countByIdUsuarioAndReadAtIsNull(idUsuario: UUID): Long
}

interface NotificationPreferenceJpaRepository : JpaRepository<NotificationPreferenceEntity, UUID> {
    fun findByIdUsuario(idUsuario: UUID): Optional<NotificationPreferenceEntity>
}
