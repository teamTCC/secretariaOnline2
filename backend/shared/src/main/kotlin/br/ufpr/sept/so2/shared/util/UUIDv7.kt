package br.ufpr.sept.so2.shared.util

import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

object UUIDv7 {
    private val rng = SecureRandom()

    fun generate(): UUID {
        val epochMs = Instant.now().toEpochMilli()

        val hi =
            (epochMs shl 16) or
                (0x7000L) or // version 7
                (rng.nextLong() and 0x0FFFL)

        val lo =
            (0x8000000000000000UL.toLong()) or
                (rng.nextLong() and 0x3FFFFFFFFFFFFFFFL)

        return UUID(hi, lo)
    }
}
