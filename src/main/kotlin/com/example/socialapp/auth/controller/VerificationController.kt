package com.example.socialapp.auth.controller

import com.example.socialapp.account.ProfileService
import com.example.socialapp.account.dto.*
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.mapper.toResponse
import com.example.socialapp.auth.AuthService
import com.example.socialapp.auth.VerificationService
import com.example.socialapp.auth.dto.*
import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.toUuid
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tags(Tag(name = "Common"), Tag(name = "Auth"))
@RestController
@RequestMapping("/api")
class VerificationController(
    private val verificationService: VerificationService,
    private val authService: AuthService,
    private val profileService: ProfileService,
) {

    @PostMapping("/auth/email/verify")
    fun verifyEmail(
        @Valid @RequestBody body: VerifyEmailRequest,
    ): ApiResponse<AuthResponse.Authenticated> {
        val account = verificationService.verifyEmail(body.email, body.code)
        val role = Role.USER
        val tokens = authService.issueTokensForAccount(account, role)
        return ApiResponse.ok(
            AuthResponse.Authenticated(
                user = account.toResponse(role, profileService.getOrEmpty(account.id!!, role)),
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            )
        )
    }

    @PostMapping("/auth/email/resend")
    fun resendEmailVerification(
        @Valid @RequestBody body: ResendVerificationRequest,
    ): ApiResponse<String> =
        ApiResponse.ok(verificationService.requestEmailVerification(body.email))

    @Tags(Tag(name = "ResetPassword"))
    @PostMapping("/auth/password-reset/request")
    fun requestPasswordReset(
        @Valid @RequestBody body: PasswordResetRequest,
    ): ApiResponse<String> =
        ApiResponse.ok(verificationService.requestPasswordReset(body.email))

    @Tags(Tag(name = "ResetPassword"))
    @PostMapping("/auth/password-reset/verify-code")
    fun verifyPasswordResetCode(
        @Valid @RequestBody body: PasswordResetVerifyCodeRequest,
    ): ApiResponse<PasswordResetTokenResponse> {
        val token = verificationService.verifyPasswordResetCode(body.email, body.code)
        return ApiResponse.ok(PasswordResetTokenResponse(resetToken = token))
    }

    @Tags(Tag(name = "ResetPassword"))
    @PostMapping("/auth/password-reset/confirm")
    fun confirmPasswordReset(
        @Valid @RequestBody body: PasswordResetConfirmRequest,
    ): ApiResponse<Unit> {
        verificationService.confirmPasswordReset(body.resetToken, body.newPassword)
        return ApiResponse.ok(Unit)
    }

    @Tags(Tag(name = "ChangeEmail"))
    @PostMapping("/profile/me/email/request")
    fun requestEmailChange(
        @Valid @RequestBody body: ChangeEmailRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<String> =
        ApiResponse.ok(verificationService.requestEmailChange(currentUserId.toUuid(), body.newEmail))

    @Tags(Tag(name = "ChangeEmail"))
    @PostMapping("/profile/me/email/verify-code")
    fun verifyEmailChangeCode(
        @Valid @RequestBody body: VerifyEmailChangeCodeRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<EmailChangeTokenResponse> {
        val token = verificationService.verifyEmailChangeCode(currentUserId.toUuid(), body.code)
        return ApiResponse.ok(EmailChangeTokenResponse(emailChangeToken = token))
    }

    @Tags(Tag(name = "ChangeEmail"))
    @PostMapping("/profile/me/email/confirm")
    fun confirmEmailChange(
        @Valid @RequestBody body: ConfirmEmailChangeRequest,
        @AuthenticationPrincipal currentUserId: String,
        authentication: Authentication,
    ): ApiResponse<AccountResponse> {
        val role = authentication.authorities.firstNotNullOfOrNull { Role.fromAuthority(it.authority) }
            ?: throw ApiException.Unauthorized("error.auth.invalid_token")
        val account = verificationService.confirmEmailChange(currentUserId.toUuid(), body.token)
        return ApiResponse.ok(account.toResponse(role, profileService.getOrEmpty(account.id!!, role)))
    }
}
