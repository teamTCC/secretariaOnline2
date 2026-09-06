package br.ufpr.sept.so2.modules.iam.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(
    name = "jti_blacklist",
    indexes = [Index(name = "idx_jti_expires_at", columnList = "expires_at")],
)
class JtiBlacklistEntity(
    @Id
    @Column(nullable = false, length = 100)
    val jti: String,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
