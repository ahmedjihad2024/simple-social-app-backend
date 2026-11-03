package com.example.socialapp.common.exception

sealed class ApiException(
    val errorCode: ErrorCode,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    class NotFound(reason: String) : ApiException(ErrorCode.NOT_FOUND, reason)
    class Unauthorized(reason: String = "Unauthorized.") : ApiException(ErrorCode.UNAUTHORIZED, reason)
    class Forbidden(reason: String = "Forbidden.") : ApiException(ErrorCode.FORBIDDEN, reason)
    class Conflict(reason: String) : ApiException(ErrorCode.CONFLICT, reason)
    open class RateLimitExceeded(reason: String) : ApiException(ErrorCode.RATE_LIMIT_EXCEEDED, reason)
    open class BadRequest(reason: String) : ApiException(ErrorCode.BAD_REQUEST, reason)
    open class InternalError(reason: String, cause: Throwable? = null) : ApiException(ErrorCode.INTERNAL_ERROR, reason, cause)
}
