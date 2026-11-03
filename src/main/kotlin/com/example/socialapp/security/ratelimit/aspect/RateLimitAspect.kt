package com.example.socialapp.security.ratelimit.aspect

import com.example.socialapp.security.ratelimit.annotation.RateLimit
import com.example.socialapp.security.ratelimit.config.RateLimitProperties
import com.example.socialapp.security.ratelimit.core.RateLimitExceededException
import com.example.socialapp.security.ratelimit.core.RateLimitPolicy
import com.example.socialapp.security.ratelimit.core.RateLimitService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class RateLimitAspect(
    private val properties: RateLimitProperties,
    private val rateLimitService: RateLimitService
) {

    @Around("@annotation(rateLimit)")
    fun enforce(
        joinPoint: ProceedingJoinPoint,
        rateLimit: RateLimit
    ): Any? {

        if (!properties.enabled) {
            return joinPoint.proceed()
        }

        val attributes =
            RequestContextHolder.getRequestAttributes()
                    as? ServletRequestAttributes
                ?: return joinPoint.proceed()

        val request: HttpServletRequest = attributes.request
        val response: HttpServletResponse? = attributes.response

        val configuredPolicy = properties.policies[rateLimit.policy]
            ?: throw IllegalStateException(
                "Unknown rate-limit policy: ${rateLimit.policy}"
            )

        val policy = RateLimitPolicy(
            capacity = configuredPolicy.capacity,
            refillTokens = configuredPolicy.refillTokens,
            refillDuration = configuredPolicy.refillDuration
        )

        val strategy = configuredPolicy.key

        val result = rateLimitService.check(
            request,
            policy,
            strategy
        )

        response?.setHeader(
            "X-RateLimit-Limit",
            result.limit.toString()
        )

        response?.setHeader(
            "X-RateLimit-Remaining",
            result.remaining.toString()
        )

        if (!result.allowed) {
            response?.setHeader(
                "Retry-After",
                result.retryAfterSeconds.toString()
            )

            throw RateLimitExceededException(result)
        }

        return joinPoint.proceed()
    }
}