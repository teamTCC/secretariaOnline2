package br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, UUID> {
    /**
     * Pessimistic write + timeout -2 = SKIP LOCKED (Hibernate), para duas instâncias
     * do dispatcher não processarem a mesma linha.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        SELECT o FROM OutboxEventEntity o
        WHERE o.status = 'PENDING'
        AND o.nextAttemptAt <= :now
        ORDER BY o.nextAttemptAt ASC
        """,
    )
    fun findPendingEvents(
        @Param("now") now: OffsetDateTime,
        pageable: Pageable,
    ): List<OutboxEventEntity>

    @Modifying
    @Query(
        """
        UPDATE OutboxEventEntity o
        SET o.status = 'PROCESSED', o.processedAt = :now
        WHERE o.id = :id
        """,
    )
    fun markProcessed(
        @Param("id") id: UUID,
        @Param("now") now: OffsetDateTime,
    )

    /** Admin inspection: list events by status without the SKIP_LOCKED pessimistic write. */
    fun findAllByStatusOrderByCreatedAtDesc(
        status: String,
        pageable: Pageable,
    ): org.springframework.data.domain.Page<OutboxEventEntity>
}
