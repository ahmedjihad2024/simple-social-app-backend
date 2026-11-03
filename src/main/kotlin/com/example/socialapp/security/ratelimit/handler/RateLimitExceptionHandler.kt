package com.example.socialapp.security.ratelimit.handler

import com.example.socialapp.security.ratelimit.core.RateLimitExceededException
import com.example.socialapp.security.ratelimit.core.RateLimitStoreUnavailableException
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RateLimitExceptionHandler(
    private val errorWriter: RateLimitErrorWriter
) {

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceeded(
        exception: RateLimitExceededException,
        response: HttpServletResponse
    ) {
        response.setHeader(
            "Retry-After",
            exception.result.retryAfterSeconds.toString()
        )

        errorWriter.write(response)
    }

    @ExceptionHandler(RateLimitStoreUnavailableException::class)
    fun handleStoreUnavailable(
        response: HttpServletResponse
    ) {
        errorWriter.writeUnavailable(response)
    }
}