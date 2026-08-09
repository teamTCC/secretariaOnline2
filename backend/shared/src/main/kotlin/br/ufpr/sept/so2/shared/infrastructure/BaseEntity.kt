package br.ufpr.sept.so2.shared.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PreUpdate
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    open val id: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false, updatable = false)
    open val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = OffsetDateTime.now()
    }
}
