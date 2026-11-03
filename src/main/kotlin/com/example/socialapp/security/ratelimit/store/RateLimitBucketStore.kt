package com.example.socialapp.security.ratelimit.store

import com.example.socialapp.security.ratelimit.core.RateLimitPolicy
import io.github.bucket4j.Bucket

interface RateLimitBucketStore {

    fun resolveBucket(
        key: String,
        policy: RateLimitPolicy
    ): Bucket
}