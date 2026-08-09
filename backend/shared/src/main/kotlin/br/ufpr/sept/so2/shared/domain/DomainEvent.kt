package br.ufpr.sept.so2.shared.domain

import java.time.OffsetDateTime
import java.util.UUID

abstract class DomainEvent {
    val eventId: UUID = UUID.randomUUID()
    val occurredAt: OffsetDateTime = OffsetDateTime.now()
}
