package com.example.socialapp.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PasswordResetConfirmRequest(
    @field:Schema(description = "Short-lived JWT reset token obtained from /api/auth/password-reset/verify-code")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    val resetToken: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 8, max = 100, message = "{validation.password.size}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+\$",
        message = "{validation.password.pattern}",
    )
    val newPassword: String,
)
