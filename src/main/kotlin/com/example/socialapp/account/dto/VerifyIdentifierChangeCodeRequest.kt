package com.example.socialapp.account.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class VerifyIdentifierChangeCodeRequest(
    @field:Schema(example = "111111")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Pattern(regexp = "^\\d{6}\$", message = "{validation.verification_code.pattern}")
    val code: String,
)
