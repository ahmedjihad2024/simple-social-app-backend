package com.example.socialapp.auth.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
