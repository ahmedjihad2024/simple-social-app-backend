package com.example.socialapp.account

import com.example.socialapp.account.dto.AccountResponse
import com.example.socialapp.account.dto.IdentifierChangeTokenResponse
import com.example.socialapp.account.entities.Account
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.lifecycle.PersonaRoleDeactivated
import com.example.socialapp.account.lifecycle.PersonaRoleRestored
import com.example.socialapp.account.mapper.toResponse
import com.example.socialapp.account.repository.AccountRepository
import com.example.socialapp.auth.VerificationService
import com.example.socialapp.auth.dto.AuthResponse
import com.example.socialapp.identity.IdentityService
import com.example.socialapp.auth.repository.RefreshTokenRepository
import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.toUuid
import com.example.socialapp.common.storage.FileCategory
import com.example.socialapp.common.storage.FileStorageService
import com.example.socialapp.security.jwt.JwtService
import com.example.socialapp.security.jwt.token_block_list.TokenBlacklist
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val verificationService: VerificationService,
    private val fileStorageService: FileStorageService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val profileService: ProfileService,
    private val events: ApplicationEventPublisher,
    private val tokenBlacklist: TokenBlacklist,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val identityService: IdentityService,
) {
    fun me(id: String, role: Role): ApiResponse<AccountResponse> {
        val user = accountRepository.findById(id.toUuid())
            .orElseThrow { ApiException.NotFound("error.account.not_found") }
        val userId = user.requireId()
        return ApiResponse.ok(
            user.toResponse(role, profileService.getOrEmpty(userId, role), identityService.get(userId))
        )
    }

    @Transactional
    fun deleteMe(id: String, role: Role, authHeader: String): ApiResponse<Unit> {
        val account = getAccount(id)
        validateRolePresent(account, role)
        softDeletePersona(account, role, authHeader)
        return ApiResponse.ok(Unit)
    }

    private fun getAccount(id: String): Account =
        accountRepository.findById(id.toUuid())
            .orElseThrow { ApiException.NotFound("error.account.not_found") }

    private fun validateRolePresent(account: Account, role: Role) {
        if (role !in account.roles) {
            throw ApiException.BadRequest("error.account.role_not_present")
        }
    }

    private fun softDeletePersona(account: Account, role: Role, authHeader: String) {
        val accountId = account.requireId()
        profileService.markDeleted(accountId, role, Instant.now())
        account.roles.remove(role)
        accountRepository.save(account)
        refreshTokenRepository.deleteAllByAccountId(accountId)
        val jti = jwtService.getJti(authHeader)
        tokenBlacklist.revoke(jti, expiresAt = Instant.now().plus(30, ChronoUnit.DAYS))
        events.publishEvent(PersonaRoleDeactivated(accountId, role))
    }

    /**
     * Restores a persona that was soft-deleted but is still within its grace period: clears the
     * profile's deleted flag and re-adds the [role]. A no-op if the persona isn't deleted. Call this
     * when an account re-authenticates (or re-registers) as that role.
     */
    @Transactional
    fun restoreRole(accountId: UUID, role: Role) {
        val account = accountRepository.findById(accountId).orElse(null) ?: return
        if (!profileService.isDeleted(accountId, role)) return

        profileService.clearDeleted(accountId, role)
        account.roles.add(role)
        accountRepository.save(account)
        events.publishEvent(PersonaRoleRestored(accountId, role))
    }

    @Transactional
    fun uploadAvatar(id: String, role: Role, file: MultipartFile): ApiResponse<AccountResponse> {
        val user = accountRepository.findById(id.toUuid())
            .orElseThrow { ApiException.NotFound("error.account.not_found") }
        val newFilename = fileStorageService.store(FileCategory.AVATAR, file)
        val userId = user.requireId()
        val previousFilename = profileService.updateAvatar(userId, role, newFilename)
        previousFilename?.let { fileStorageService.delete(FileCategory.AVATAR, it) }
        return ApiResponse.ok(
            user.toResponse(role, profileService.getOrEmpty(userId, role), identityService.get(userId))
        )
    }

    @Transactional
    fun updateProfile(
        id: String,
        firstName: String?,
        lastName: String?,
        role: Role,
    ): ApiResponse<AccountResponse> {
        val user = accountRepository.findById(id.toUuid())
            .orElseThrow { ApiException.NotFound("error.account.not_found") }
        val userId = user.requireId()
        val profile = profileService.upsertProfile(userId, role, firstName, lastName)
        return ApiResponse.ok(user.toResponse(role, profile, identityService.get(userId)))
    }

    /** Request changing the email / phone: delegates to unified verification service. */
    @Transactional
    fun changeIdentifier(id: String, newIdentifier: String): ApiResponse<AuthResponse.SetUpRequired> {
        val message = verificationService.requestIdentifierChange(id.toUuid(), newIdentifier)
        return ApiResponse.ok(
            AuthResponse.SetUpRequired(
                identifier = identityService.normalize(newIdentifier),
                identifierType = identityService.type,
                message = message,
            ),
        )
    }

    /** Verifies the identifier change code: returns short-lived change token. */
    @Transactional
    fun verifyChangeIdentifierCode(id: String, code: String): ApiResponse<IdentifierChangeTokenResponse> {
        val token = verificationService.verifyIdentifierChangeCode(id.toUuid(), code)
        return ApiResponse.ok(IdentifierChangeTokenResponse(identifierChangeToken = token))
    }

    /** Confirms the change: consumes the short-lived token and updates the account's identifier. */
    @Transactional
    fun confirmIdentifierChange(id: String, token: String, role: Role): ApiResponse<AccountResponse> {
        val account = verificationService.confirmIdentifierChange(id.toUuid(), token)
        val accountId = account.requireId()
        return ApiResponse.ok(
            account.toResponse(role, profileService.getOrEmpty(accountId, role), identityService.get(accountId))
        )
    }

    @Transactional
    fun changePassword(id: String, currentPassword: String, newPassword: String): ApiResponse<Unit> {
        val user = getAccount(id)
        if (!passwordEncoder.matches(currentPassword, user.hashPassword)) {
            throw ApiException.BadRequest("error.account.invalid_current_password")
        }
        if (passwordEncoder.matches(newPassword, user.hashPassword)) {
            throw ApiException.BadRequest("error.account.password_same_as_current")
        }
        user.hashPassword = passwordEncoder.encode(newPassword)!!
        accountRepository.save(user)
        refreshTokenRepository.deleteAllByAccountId(user.requireId())
        return ApiResponse.ok(Unit)
    }

    private fun Account.requireId() =
        id ?: throw IllegalStateException("Account must be persisted before its ID is used")
}
