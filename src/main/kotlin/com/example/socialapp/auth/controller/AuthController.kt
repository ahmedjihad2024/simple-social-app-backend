package com.example.socialapp.auth.controller

import com.example.socialapp.auth.AuthService
import com.example.socialapp.auth.dto.AuthResponse
import com.example.socialapp.auth.dto.RefreshRequest
import com.example.socialapp.auth.dto.ResendVerificationRequest
import com.example.socialapp.auth.dto.TokenResponse
import com.example.socialapp.auth.dto.VerifyEmailRequest
import com.example.socialapp.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tags(Tag(name = "Common"), Tag(name = "Auth"))
@RestController
@RequestMapping("api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody body: RefreshRequest): ApiResponse<TokenResponse> =
        ApiResponse.ok(authService.refresh(body.refreshToken))

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @Valid @RequestBody body: RefreshRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authHeader: String?,
    ) {
        val accessToken = authHeader?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        authService.logout(body.refreshToken, accessToken)
    }
}
