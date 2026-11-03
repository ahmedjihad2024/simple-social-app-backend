package com.example.socialapp.security.ratelimit.filter

import com.example.socialapp.security.ratelimit.config.RateLimitProperties
import com.example.socialapp.security.ratelimit.core.RateLimitExceededException
import com.example.socialapp.security.ratelimit.core.RateLimitPolicy
import com.example.socialapp.security.ratelimit.core.RateLimitService
import com.example.socialapp.security.ratelimit.core.RateLimitStoreUnavailableException
import com.example.socialapp.security.ratelimit.handler.RateLimitErrorWriter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class GlobalRateLimitFilter(
    private val properties: RateLimitProperties,
    private val rateLimitService: RateLimitService,
    private val errorWriter: RateLimitErrorWriter
) : OncePerRequestFilter() {

    override fun shouldNotFilter(
        request: HttpServletRequest
    ): Boolean {
        return !properties.enabled ||
                !request.requestURI.startsWith("/api/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val configured = properties.global

        val policy = RateLimitPolicy(
            capacity = configured.capacity,
            refillTokens = configured.refillTokens,
            refillDuration = configured.refillDuration
        )

        try {
            val result = rateLimitService.check(
                request,
                policy,
                configured.key,
                endpointScoped = false
            )

            response.setHeader(
                "X-RateLimit-Limit",
                result.limit.toString()
            )

            response.setHeader(
                "X-RateLimit-Remaining",
                result.remaining.toString()
            )

            if (!result.allowed) {
                response.setHeader(
                    HttpHeaders.RETRY_AFTER,
                    result.retryAfterSeconds.toString()
                )

                errorWriter.write(response)
                return
            }

            filterChain.doFilter(request, response)

        } catch (exception: RateLimitStoreUnavailableException) {
            errorWriter.writeUnavailable(response)
        }
    }
}