package com.example.socialapp.account.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = AccountResponse.UserResponse::class, name = "USER")
)
sealed class AccountResponse {
    data class UserResponse(
        val id: String,
        val firstName: String?,
        val lastName: String?,
        val email: String,
        val emailVerified: Boolean,
        val avatar: String? = null,
    ) : AccountResponse()
}
