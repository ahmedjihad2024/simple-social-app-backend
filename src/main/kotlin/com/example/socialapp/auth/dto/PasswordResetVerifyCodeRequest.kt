package com.example.socialapp.auth.dto

import com.example.socialapp.identity.ValidIdentifier
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class PasswordResetVerifyCodeRequest(
    @field:Schema(description = "Email or phone number, depending on app.auth.identifier", example = "user@example.com")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:ValidIdentifier
    val identifier: String,

    @field:Schema(example = "111111")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Pattern(regexp = "^\\d{6}\$", message = "{validation.verification_code.pattern}")
    val code: String,
)
