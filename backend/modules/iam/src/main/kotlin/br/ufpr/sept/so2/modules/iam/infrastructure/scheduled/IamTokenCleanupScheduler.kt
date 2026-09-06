package br.ufpr.sept.so2.modules.iam.infrastructure.scheduled

import br.ufpr.sept.so2.modules.iam.application.ports.out.EmailOneTimeTokenStore
import br.ufpr.sept.so2.modules.iam.application.ports.out.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Nightly cleanup of stale auth records.
 *
 * Both tables accumulate rows that are kept past expiry for audit/diagnostics,
 * but once expired they serve no functional purpose and bloat the DB.
 * The cleanup runs at 03:00 (server TZ) to avoid peak-hour load.
 */
@Component
class IamTokenCleanupScheduler(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailOneTimeTokenStore: EmailOneTimeTokenStore,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanupExpiredTokens() {
        log.info("IAM token cleanup started")
        try {
            refreshTokenRepository.deleteExpired()
            log.info("Expired refresh tokens purged")
        } catch (e: Exception) {
            log.error("Failed to purge expired refresh tokens: {}", e.message, e)
        }
        try {
            emailOneTimeTokenStore.deleteExpired()
            log.info("Expired email one-time tokens purged")
        } catch (e: Exception) {
            log.error("Failed to purge expired JTI blacklist entries: {}", e.message, e)
        }
        log.info("IAM token cleanup finished")
    }
}
