package com.example.socialapp.account.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ConfirmIdentifierChangeRequest(
    @field:Schema(description = "Short-lived JWT identifier change token obtained from verify-code endpoint")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    val token: String,
)
