package com.example.socialapp.auth.dto

import jakarta.validation.constraints.Email
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

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Email(message = "{validation.invalid_email}")
    val email: String,

    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 8, max = 100, message = "{validation.password.size}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+\$",
        message = "{validation.password.pattern}",
    )
    val password: String,
)
