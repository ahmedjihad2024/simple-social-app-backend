package com.example.socialapp.auth.dto

import com.example.socialapp.identity.ValidIdentifier
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size


data class UserRegisterRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 2, max = 25, message = "{validation.name.size}")
    val firstName: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 2, max = 25, message = "{validation.name.size}")
    val lastName: String,

    @field:Schema(description = "Email or phone number (international format, e.g. +201234567890), depending on app.auth.identifier")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:ValidIdentifier
    val identifier: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 8, max = 100, message = "{validation.password.size}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+\$",
        message = "{validation.password.pattern}",
    )
    val password: String,
)
