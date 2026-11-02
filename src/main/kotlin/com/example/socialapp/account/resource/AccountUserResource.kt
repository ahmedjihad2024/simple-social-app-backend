package com.example.socialapp.account.resource

import com.example.socialapp.account.ProfileData
import com.example.socialapp.account.ProfileService
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.lifecycle.PersonaOwnedResource
import com.example.socialapp.common.storage.FileCategory
import com.example.socialapp.common.storage.FileStorageService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AccountUserResource(
    private val profileService: ProfileService,
    private val fileStorageService: FileStorageService,
) : PersonaOwnedResource {
    override val role = Role.USER

    override fun preparePurge(accountId: UUID): List<() -> Unit> {
        val avatarFilename = (profileService.get(accountId, role) as? ProfileData.UserProfileData)
            ?.avatarFilename
            ?: return emptyList()

        return listOf {
            fileStorageService.delete(FileCategory.AVATAR, avatarFilename)
        }
    }
}
