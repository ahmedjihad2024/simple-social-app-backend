package com.example.socialapp.security.ratelimit.core

import java.time.Duration

data class RateLimitPolicy (
    val capacity: Long,
    val refillTokens: Long,
    val refillDuration: Duration
)