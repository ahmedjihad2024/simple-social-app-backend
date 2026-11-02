package com.example.socialapp.auth.scheduler

import com.example.socialapp.auth.repository.RefreshTokenRepository
import com.example.socialapp.auth.repository.VerificationCodeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class AuthCleanup(
    private val verificationCodeRepo: VerificationCodeRepository,
    private val refreshTokenRepo: RefreshTokenRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Runs every 10 minutes: cleans expired verification codes from the unified verification_codes table
    @Scheduled(fixedRate = 600_000)
    @Transactional
    fun cleanVerificationCodes() {
        val deleted = verificationCodeRepo.deleteExpired(Instant.now())
        if (deleted > 0) log.info("Deleted {} expired verification codes", deleted)
    }

    // Runs daily at 03:00 server time: cleans expired refresh tokens
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanRefreshTokens() {
        val deleted = refreshTokenRepo.deleteExpired(Instant.now())
        if (deleted > 0) log.info("Deleted {} expired refresh tokens", deleted)
    }
}
