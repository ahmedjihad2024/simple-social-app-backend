package com.example.socialapp.auth.senders

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.SecureRandom

const val VERIFICATION_CODE_TTL_MINUTES: Long = 15L

interface VerificationCodeGenerator {
    // 6-digit code (000000..999999). Matches the DTO regex `^\d{6}$`.
    fun generate(): String
}

/** Dev/test generator: returns predictable code (111111) when app.mail.code=fixed or missing. */
@Component
@ConditionalOnProperty(name = ["app.mail.code"], havingValue = "fixed", matchIfMissing = true)
class FixedVerificationCodeGenerator : VerificationCodeGenerator {
    override fun generate(): String = "%06d".format(111111)
}

/** Production generator: cryptographically random 6-digit code. */
@Component
@ConditionalOnProperty(name = ["app.mail.code"], havingValue = "random")
class RandomVerificationCodeGenerator : VerificationCodeGenerator {
    private val rng = SecureRandom()

    override fun generate(): String = "%06d".format(rng.nextInt(1_000_000))
}
