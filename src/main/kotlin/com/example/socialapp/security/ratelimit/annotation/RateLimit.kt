package com.example.socialapp.security.ratelimit.annotation

import com.example.socialapp.security.ratelimit.config.RateLimitProperties.RateLimitKey

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    val policy: String,
)