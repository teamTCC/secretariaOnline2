package br.ufpr.sept.so2.modules.iam.application.ports.out

import java.time.OffsetDateTime

interface JtiBlacklistRepository {
    fun add(
        jti: String,
        expiresAt: OffsetDateTime,
    )

    fun exists(jti: String): Boolean

    fun deleteExpired()
}
