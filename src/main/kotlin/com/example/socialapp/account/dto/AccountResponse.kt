package com.example.socialapp.account.dto

import com.example.socialapp.identity.IdentifierType
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
        /** The email address or E.164 phone number the account signs in with. */
        val identifier: String,
        val identifierType: IdentifierType,
        val identifierVerified: Boolean,
        val avatar: String? = null,
    ) : AccountResponse()
}
