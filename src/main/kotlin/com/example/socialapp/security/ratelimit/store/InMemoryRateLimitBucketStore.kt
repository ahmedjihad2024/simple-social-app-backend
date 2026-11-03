package com.example.socialapp.security.ratelimit.store

import com.example.socialapp.security.ratelimit.core.RateLimitPolicy
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Profile("dev")
class InMemoryRateLimitBucketStore : RateLimitBucketStore {

    private val buckets = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, Bucket>()

    override fun resolveBucket(
        key: String,
        policy: RateLimitPolicy
    ): Bucket {

        return buckets.get(key) {
            Bucket.builder()
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
        }
    }
}