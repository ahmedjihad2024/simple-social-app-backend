package com.example.socialapp.account.dto

import io.swagger.v3.oas.annotations.media.Schema

data class EmailChangeTokenResponse(
    @field:Schema(description = "Short-lived JWT email change token valid for confirming email change")
    val emailChangeToken: String,
)
