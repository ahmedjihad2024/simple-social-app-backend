package com.example.socialapp.security.ratelimit.core

import com.example.socialapp.security.ratelimit.config.RateLimitProperties
import com.example.socialapp.security.ratelimit.store.RateLimitBucketStore
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import java.time.Duration

data class RateLimitResult(
    val allowed: Boolean,
    val limit: Long,
    val remaining: Long,
    val retryAfterSeconds: Long = 0
)

@Service
class RateLimitService(
    private val bucketStore: RateLimitBucketStore,
    private val keyResolver: RateLimitKeyResolver,
    private val properties: RateLimitProperties
) {

    fun check(
        request: HttpServletRequest,
        policy: RateLimitPolicy,
        strategy: RateLimitProperties.RateLimitKey,
        endpointScoped: Boolean = true
    ): RateLimitResult {

        val key = keyResolver.resolve(request, strategy, endpointScoped)

        return try {
            val bucket = bucketStore.resolveBucket(
                key = key,
                policy = policy
            )

            val probe = bucket.tryConsumeAndReturnRemaining(1)

            val retryAfter = if (probe.isConsumed) {
                0L
            } else {
                nanosToSecondsCeiling(probe.nanosToWaitForRefill)
            }

            RateLimitResult(
                allowed = probe.isConsumed,
                limit = policy.capacity,
                remaining = probe.remainingTokens,
                retryAfterSeconds = retryAfter
            )
        } catch (exception: Exception) {
            when (properties.failureStrategy) {
                RateLimitProperties.FailureStrategy.FAIL_OPEN -> {
                    RateLimitResult(
                        allowed = true,
                        limit = policy.capacity,
                        remaining = policy.capacity
                    )
                }

                RateLimitProperties.FailureStrategy.FAIL_CLOSED -> {
                    throw RateLimitStoreUnavailableException(exception)
                }
            }
        }
    }

    private fun nanosToSecondsCeiling(nanos: Long): Long {
        if (nanos <= 0) return 0

        return Duration.ofNanos(nanos).seconds +
                if (nanos % 1_000_000_000L != 0L) 1 else 0
    }
}