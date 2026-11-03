package com.example.socialapp.security.ratelimit.store

import com.example.socialapp.security.ratelimit.core.RateLimitPolicy
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!dev")
class RedisRateLimitBucketStore(
    private val proxyManager: LettuceBasedProxyManager<String>
) : RateLimitBucketStore {

    override fun resolveBucket(
        key: String,
        policy: RateLimitPolicy
    ): Bucket {

        val configuration = BucketConfiguration.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(policy.capacity)
                    .refillIntervally(
                        policy.refillTokens,
                        policy.refillDuration
                    )
                    .build()
            )
            .build()

        return proxyManager.builder()
            .build(key) { configuration }
    }
}