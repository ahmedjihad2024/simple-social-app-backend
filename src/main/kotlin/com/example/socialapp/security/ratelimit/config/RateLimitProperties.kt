package com.example.socialapp.security.ratelimit.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,

    val failureStrategy: FailureStrategy = FailureStrategy.FAIL_OPEN,

    val global: Policy = Policy(
        capacity = 100,
        refillTokens = 100,
        refillDuration = Duration.ofMinutes(1),
        key = RateLimitKey.USER
    ),

    val policies: Map<String, Policy> = emptyMap(),

    val trustedProxyIps: Set<String> = setOf(
        "127.0.0.1",
        "::1"
    )
) {

    init {
        validatePolicy(
            name = "global",
            capacity = global.capacity,
            refillTokens = global.refillTokens,
            duration = global.refillDuration
        )

        policies.forEach { (name, policy) ->
            validatePolicy(
                name = name,
                capacity = policy.capacity,
                refillTokens = policy.refillTokens,
                duration = policy.refillDuration
            )
        }
    }

    private fun validatePolicy(
        name: String,
        capacity: Long,
        refillTokens: Long,
        duration: Duration
    ) {
        require(capacity > 0) {
            "$name capacity must be greater than zero"
        }

        require(refillTokens > 0) {
            "$name refillTokens must be greater than zero"
        }

        require(!duration.isZero && !duration.isNegative) {
            "$name duration must be positive"
        }
    }


    enum class FailureStrategy {
        FAIL_OPEN,
        FAIL_CLOSED
    }

    data class Policy(
        val capacity: Long,
        val refillTokens: Long,
        val refillDuration: Duration,
        val key: RateLimitKey = RateLimitKey.IP
    )

    enum class RateLimitKey {
        IP,
        USER,
        IP_AND_USER
    }
}