package br.ufpr.sept.so2.modules.presenca.application.ports.out

import java.time.OffsetDateTime
import java.util.UUID

data class EventSearchHit(
    val id: UUID,
    val titulo: String,
    val estado: String,
)

data class UpcomingEventHit(
    val id: UUID,
    val titulo: String,
    val inicioEm: OffsetDateTime,
    val estado: String,
)

interface PresencaBffReadPort {
    fun countByEstado(estado: String): Long

    fun findUpcoming(
        from: OffsetDateTime,
        limit: Int,
    ): List<UpcomingEventHit>

    fun searchByTitulo(
        q: String,
        page: Int,
        size: Int,
    ): List<EventSearchHit>
}
