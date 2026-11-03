package com.example.socialapp.account.dto

import io.swagger.v3.oas.annotations.media.Schema

data class IdentifierChangeTokenResponse(
    @field:Schema(description = "Short-lived JWT identifier change token valid for confirming the change")
    val identifierChangeToken: String,
)
