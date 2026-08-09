package br.ufpr.sept.so2.modules.notificacoes.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, UUID> {
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

    @Modifying
    @Query(
        """
        UPDATE OutboxEventEntity o
        SET o.status = 'FAILED', o.lastError = :error, o.attemptCount = o.attemptCount + 1,
            o.nextAttemptAt = :nextAttempt
        WHERE o.id = :id
    """,
    )
    fun markFailed(
        @Param("id") id: UUID,
        @Param("error") error: String,
        @Param("nextAttempt") nextAttempt: OffsetDateTime,
    )
}
