package com.example.socialapp.auth.controller

import com.example.socialapp.account.ProfileService
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.mapper.toResponse
import com.example.socialapp.auth.AuthService
import com.example.socialapp.auth.VerificationService
import com.example.socialapp.auth.dto.*
import com.example.socialapp.identity.IdentityService
import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.security.ratelimit.annotation.RateLimit
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Verification + password reset. Identifier change (email / phone) lives in
 * [com.example.socialapp.account.controller.AccountController] under /api/profile/me/change-identifier.
 */
@Tags(Tag(name = "Common"), Tag(name = "Auth"))
@RestController
@RequestMapping("/api")
class VerificationController(
    private val verificationService: VerificationService,
    private val authService: AuthService,
    private val profileService: ProfileService,
    private val identityService: IdentityService,
) {

    @RateLimit(policy = "verify")
    @PostMapping("/auth/verify")
    fun verify(
        @Valid @RequestBody body: VerifyRequest,
    ): ApiResponse<AuthResponse.Authenticated> {
        val account = verificationService.verify(body.identifier, body.code)
        val role = Role.USER
        val tokens = authService.issueTokensForAccount(account, role)
        val accountId = account.id!!
        return ApiResponse.ok(
            AuthResponse.Authenticated(
                user = account.toResponse(role, profileService.getOrEmpty(accountId, role), identityService.get(accountId)),
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            )
        )
    }

    @RateLimit(policy = "resend-verification")
    @PostMapping("/auth/resend")
    fun resendVerification(
        @Valid @RequestBody body: ResendVerificationRequest,
    ): ApiResponse<String> =
        ApiResponse.ok(verificationService.requestVerification(body.identifier))

    @RateLimit(policy = "password-reset-request")
    @Tags(Tag(name = "ResetPassword"))
    @PostMapping("/auth/password-reset/request")
    fun requestPasswordReset(
        @Valid @RequestBody body: PasswordResetRequest,
    ): ApiResponse<String> =
        ApiResponse.ok(verificationService.requestPasswordReset(body.identifier))

    @RateLimit(policy = "password-reset-verify")
    @Tags(Tag(name = "ResetPassword"))
    @PostMapping("/auth/password-reset/verify-code")
    fun verifyPasswordResetCode(
        @Valid @RequestBody body: PasswordResetVerifyCodeRequest,
    ): ApiResponse<PasswordResetTokenResponse> {
        val token = verificationService.verifyPasswordResetCode(body.identifier, body.code)
        return ApiResponse.ok(PasswordResetTokenResponse(resetToken = token))
    }

    @RateLimit(policy = "password-reset-confirm")
    @Tags(Tag(name = "ResetPassword"))
    @PostMapping("/auth/password-reset/confirm")
    fun confirmPasswordReset(
        @Valid @RequestBody body: PasswordResetConfirmRequest,
    ): ApiResponse<Unit> {
        verificationService.confirmPasswordReset(body.resetToken, body.newPassword)
        return ApiResponse.ok(Unit)
    }
}
