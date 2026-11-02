package com.example.socialapp.account.mapper

import com.example.socialapp.common.extentions.toMediaUrl
import com.example.socialapp.common.storage.FileCategory
import com.example.socialapp.account.dto.AccountResponse
import com.example.socialapp.account.entities.Account
import com.example.socialapp.account.entities.UserProfile
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.ProfileData
import com.example.socialapp.common.exception.ApiException


fun Account.toResponse(role: Role, profile: ProfileData): AccountResponse {
    val id = id.toString()
    return when (role) {
        Role.USER if profile is ProfileData.UserProfileData -> {
            AccountResponse.UserResponse(
                id = id, firstName = profile.firstName, lastName = profile.lastName,
                email = email, emailVerified = profile.emailVerified,
                avatar = profile.avatarFilename.toMediaUrl(FileCategory.AVATAR),
            )
        }
        else -> {
            throw ApiException.NotFound("error.account.not_found")
        }
    }
}

fun UserProfile.toProfileData() = ProfileData.UserProfileData(
    firstName, lastName, avatarFilename, emailVerified,
)