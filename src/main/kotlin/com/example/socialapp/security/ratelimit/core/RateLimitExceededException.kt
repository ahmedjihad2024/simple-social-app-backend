package com.example.socialapp.security.ratelimit.core

import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.exception.ErrorCode

class RateLimitExceededException(
    val result: RateLimitResult
) : ApiException.RateLimitExceeded("Rate limit exceeded")

class RateLimitStoreUnavailableException(
    cause: Throwable
) : ApiException.InternalError("Rate limit storage unavailable", cause)