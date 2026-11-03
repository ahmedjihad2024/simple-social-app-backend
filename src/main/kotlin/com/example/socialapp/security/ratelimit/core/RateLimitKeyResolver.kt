package com.example.socialapp.security.ratelimit.core

import com.example.socialapp.security.ratelimit.config.RateLimitProperties
import com.example.socialapp.security.ratelimit.config.RateLimitProperties.RateLimitKey
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import java.security.MessageDigest

@Component
class RateLimitKeyResolver(
    private val properties: RateLimitProperties
) {

    fun resolve(
        request: HttpServletRequest,
        strategy: RateLimitKey,
        endpointScoped: Boolean = true
    ): String {

        val ip = resolveClientIp(request)
        val user = resolveUser()

        val identity = when (strategy) {
            RateLimitKey.IP -> "ip:$ip"
            RateLimitKey.USER -> "user:${user ?: "ip:$ip"}"
            RateLimitKey.IP_AND_USER ->
                "ip:$ip:user:${user ?: "anonymous"}"
        }

        val endpoint = if (endpointScoped) {
            ":${buildEndpointIdentity(request)}"
        } else {
            ""
        }

        return hash("$identity$endpoint")
    }

    private fun resolveUser(): String? {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: return null

        if (!authentication.isAuthenticated ||
            authentication is AnonymousAuthenticationToken
        ) {
            return null
        }

        return authentication.name.takeIf { it.isNotBlank() }
    }

    private fun resolveClientIp(
        request: HttpServletRequest
    ): String {

        val remoteAddress = request.remoteAddr ?: "unknown"

        if (remoteAddress !in properties.trustedProxyIps) {
            return remoteAddress
        }

        // The reverse proxy must overwrite X-Forwarded-For
        // rather than append untrusted client-provided values.
        return request.getHeader("X-Forwarded-For")
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: remoteAddress
    }

    private fun buildEndpointIdentity(
        request: HttpServletRequest
    ): String {

        val pattern = request.getAttribute(
            HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        )?.toString() ?: request.servletPath

        return "${request.method}:$pattern"
    }

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}