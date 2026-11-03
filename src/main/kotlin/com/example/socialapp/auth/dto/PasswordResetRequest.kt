package com.example.socialapp.auth.dto

import com.example.socialapp.identity.ValidIdentifier
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class PasswordResetRequest(
    @field:Schema(description = "Email or phone number, depending on app.auth.identifier", example = "user@example.com")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:ValidIdentifier
    val identifier: String,
)
