package com.example.socialapp.account.dto

import com.example.socialapp.identity.ValidIdentifier
import jakarta.validation.constraints.NotBlank

data class ChangeIdentifierRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:ValidIdentifier
    val newIdentifier: String,
)
