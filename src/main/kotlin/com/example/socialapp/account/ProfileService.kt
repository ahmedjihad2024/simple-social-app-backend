package com.example.socialapp.account

import com.example.socialapp.account.entities.UserProfile
import com.example.socialapp.account.repository.AccountRepository
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.mapper.toProfileData
import com.example.socialapp.account.repository.UserProfileRepository
import com.example.socialapp.common.exception.ApiException

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

sealed class ProfileData {
    data class UserProfileData(
         val firstName: String? = null,
         val lastName: String? = null,
         val avatarFilename: String? = null,
         val emailVerified: Boolean = false,
    ): ProfileData()
}

/** A persona whose deletion grace period has elapsed — the purge work list. */
data class DeletedPersona(val accountId: UUID, val role: Role)


@Service
class ProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val accountRepository: AccountRepository,
) {
    /** The persona's profile data, or null if it has none. */
    fun get(accountId: UUID, role: Role): ProfileData? = when (role) {
        Role.USER -> userProfileRepository.findByAccountId(accountId)?.toProfileData()
    }

    /** Like [get] but never null — empty data for a persona with no profile yet. */
    fun getOrEmpty(accountId: UUID, role: Role): ProfileData =
        get(accountId, role) ?: when (role) {
            Role.USER -> ProfileData.UserProfileData()
        }

    /** True if this persona has verified their email. */
    fun isEmailVerified(accountId: UUID, role: Role): Boolean = when (role) {
        Role.USER -> userProfileRepository.findByAccountId(accountId)?.emailVerified == true
    }

    fun isRegistered(accountId: UUID, role: Role): Boolean = isEmailVerified(accountId, role)

    /**
     * Creates the persona profile if missing and applies any non-null name (null = leave as-is).
     * Returns the resulting data. Used by register and profile updates.
     */
    fun upsertProfile(
        accountId: UUID,
        role: Role,
        firstName: String? = null,
        lastName: String? = null,
    ): ProfileData {
        when (role) {
            Role.USER -> {
                val p = userProfileRepository.findByAccountId(accountId) ?: newProfile(accountId)
                firstName?.let { p.firstName = it }
                lastName?.let { p.lastName = it }
                userProfileRepository.save(p)
            }
        }
        return getOrEmpty(accountId, role)
    }

    /** Marks this persona email verified, creating the profile first if needed. */
    fun markEmailVerified(accountId: UUID, role: Role) {
        when (role) {
            Role.USER -> {
                val p = userProfileRepository.findByAccountId(accountId) ?: newProfile(accountId)
                p.emailVerified = true
                userProfileRepository.save(p)
            }
        }
    }

    fun markRegistered(accountId: UUID, role: Role) = markEmailVerified(accountId, role)

    /** Sets the persona's avatar, creating the profile if needed. Returns the previous filename (if any). */
    fun updateAvatar(accountId: UUID, role: Role, avatarFilename: String): String? = when (role) {
        Role.USER -> {
            val p = userProfileRepository.findByAccountId(accountId) ?: newProfile(accountId)
            val previousFilename = p.avatarFilename
            p.avatarFilename = avatarFilename
            previousFilename
        }
    }

    /** Soft-deletes a persona (sets deleted + deletedAt). No-op if the profile doesn't exist. */
    fun markDeleted(accountId: UUID, role: Role, at: Instant) {
        when (role) {
            Role.USER -> userProfileRepository.findByAccountId(accountId)?.let {
                it.deleted = true
                it.deletedAt = at
            }
        }
    }

    /** True if this persona is currently soft-deleted (within its grace period). */
    fun isDeleted(accountId: UUID, role: Role): Boolean = when (role) {
        Role.USER -> userProfileRepository.findByAccountId(accountId)?.deleted == true
    }

    /** Clears a persona's soft-delete (restore). No-op if the profile doesn't exist. */
    fun clearDeleted(accountId: UUID, role: Role) {
        when (role) {
            Role.USER -> userProfileRepository.findByAccountId(accountId)?.let {
                it.deleted = false
                it.deletedAt = null
            }
        }
    }

    /** Personas whose deletion grace period has elapsed, ready for the purge job. */
    fun findExpiredDeleted(cutoff: Instant): List<DeletedPersona> =
        userProfileRepository.findExpiredDeletedAccountIds(cutoff)
            .map { DeletedPersona(accountId = it.accountId, role = Role.USER) }

    /** True if the account still has any persona profile at all (deleted or not). */
    fun hasAnyProfile(accountId: UUID): Boolean =
        userProfileRepository.existsByAccountId(accountId)

    /** Removes one persona's profile — used when that persona is purged after its grace period. */
    fun delete(accountId: UUID, role: Role) {
        when (role) {
            Role.USER -> userProfileRepository.deleteByAccountId(accountId)
        }
    }

    private fun newProfile(accountId: UUID): UserProfile =
        UserProfile(
            account = accountRepository.getReferenceById(accountId),
        )

}
