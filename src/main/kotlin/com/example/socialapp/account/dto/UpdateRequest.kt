package com.example.socialapp.account.dto

import jakarta.validation.constraints.Size

data class UpdateRequest(
    @field:Size(min = 2, max = 50, message = "{validation.name.size}")
    val firstName: String? = null,
    val lastName: String? = null,
)
