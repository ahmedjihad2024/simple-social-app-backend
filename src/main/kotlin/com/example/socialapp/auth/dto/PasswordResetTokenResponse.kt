package com.example.socialapp.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

data class PasswordResetTokenResponse(
    @field:Schema(description = "Short-lived JWT reset token valid for confirming password reset")
    val resetToken: String,
)
