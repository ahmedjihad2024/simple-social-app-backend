package com.example.socialapp.account.controller

import com.example.socialapp.account.AccountService
import com.example.socialapp.account.dto.*
import com.example.socialapp.account.enums.Role
import com.example.socialapp.auth.dto.AuthResponse
import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.common.exception.ApiException
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tags(Tag(name = "Common"), Tag(name = "Profile"))
@RestController
@RequestMapping("/api/profile")
class AccountController(
    private val accountService: AccountService,
) {
    @PostMapping("/me/change-email/request")
    fun changeEmail(
        @Valid @RequestBody body: ChangeEmailRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<AuthResponse.SetUpRequired> =
        accountService.changeEmail(currentUserId, body.newEmail)

    @PostMapping("/me/change-email/verify-code")
    fun verifyChangeEmailCode(
        @Valid @RequestBody body: VerifyEmailChangeCodeRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<EmailChangeTokenResponse> =
        accountService.verifyChangeEmailCode(currentUserId, body.code)

    @PostMapping("/me/change-email/confirm")
    fun confirmEmailChange(
        @Valid @RequestBody body: ConfirmEmailChangeRequest,
        @AuthenticationPrincipal currentUserId: String,
        authentication: Authentication,
    ): ApiResponse<AccountResponse> {
        val role = authentication.authorities.firstNotNullOfOrNull { Role.fromAuthority(it.authority) }
            ?: throw ApiException.Unauthorized("error.auth.invalid_token")
        return accountService.confirmEmailChange(currentUserId, body.token, role)
    }

    @PostMapping("/me/password")
    fun changePassword(
        @Valid @RequestBody body: ChangePasswordRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<Unit> =
        accountService.changePassword(currentUserId, body.currentPassword, body.newPassword)
}
