package com.example.socialapp.auth.dto

import com.example.socialapp.account.dto.AccountResponse
import com.example.socialapp.identity.IdentifierType
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = AuthResponse.Authenticated::class, name = "AUTHENTICATED"),
    JsonSubTypes.Type(value = AuthResponse.SetUpRequired::class, name = "SET_UP_REQUIRED"),
)
sealed interface AuthResponse {
    data class Authenticated(
        val user: AccountResponse,
        val accessToken: String,
        val refreshToken: String,
    ) : AuthResponse

    data class SetUpRequired(
        /** The email / phone a verification code was just sent to. */
        val identifier: String,
        val identifierType: IdentifierType,
        val message: String,
        val identifierVerified: Boolean = false,
    ) : AuthResponse
}
