package com.example.socialapp.security.ratelimit.handler

import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.common.exception.ErrorCode
import com.example.socialapp.common.extentions.tr
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class RateLimitErrorWriter(
    private val objectMapper: ObjectMapper
) {

    fun write(response: HttpServletResponse) {
        if (response.isCommitted) return

        response.status = 429
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        val body = ApiResponse.fail(
            ErrorCode.RATE_LIMIT_EXCEEDED.code,
            "error.rate_limit.exceeded".tr()
        )

        objectMapper.writeValue(response.outputStream, body)
    }

    fun writeUnavailable(response: HttpServletResponse) {
        if (response.isCommitted) return

        response.status = HttpServletResponse.SC_SERVICE_UNAVAILABLE
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        objectMapper.writeValue(
            response.outputStream,
                    ApiResponse.fail(
                        "SC_SERVICE_UNAVAILABLE",
            "Rate limit service temporarily unavailable"
        )
        )
    }
}