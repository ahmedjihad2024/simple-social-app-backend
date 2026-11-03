package com.example.socialapp.auth

import com.example.socialapp.account.AccountService
import com.example.socialapp.account.ProfileService
import com.example.socialapp.account.entities.Account
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.mapper.toResponse
import com.example.socialapp.account.repository.AccountRepository
import com.example.socialapp.auth.dto.AuthResponse
import com.example.socialapp.auth.dto.TokenResponse
import com.example.socialapp.auth.entities.RefreshToken
import com.example.socialapp.identity.IdentityService
import com.example.socialapp.auth.repository.RefreshTokenRepository
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.sha256
import com.example.socialapp.common.extentions.toUuid
import com.example.socialapp.common.extentions.tr
import com.example.socialapp.security.jwt.JwtService
import com.example.socialapp.security.jwt.token_block_list.TokenBlacklist
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthService(
    private val accountRepository: AccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenBlacklist: TokenBlacklist,
    private val jwtService: JwtService,
    private val accountService: AccountService,
    private val profileService: ProfileService,
    private val passwordEncoder: PasswordEncoder,
    private val verificationService: VerificationService,
    private val identityService: IdentityService,
) {

    @Transactional
    fun register(
        firstName: String,
        lastName: String,
        identifier: String,
        password: String,
        role: Role,
    ): AuthResponse.SetUpRequired {
        val normalized = identityService.normalize(identifier)
        if (identityService.exists(normalized)) {
            throw ApiException.Conflict("error.auth.identifier_already_exists")
        }

        val user = accountRepository.save(
            Account(
                hashPassword = passwordEncoder.encode(password)!!,
                roles = mutableSetOf(role),
            )
        )
        val userId = user.requireId()

        // The identity (email or phone) starts unverified until the user enters the code.
        identityService.create(user, normalized)
        profileService.upsertProfile(userId, role, firstName, lastName)

        verificationService.requestVerification(userId)
        return AuthResponse.SetUpRequired(
            identifier = normalized,
            identifierType = identityService.type,
            message = "error.auth.verification_code_sent".tr(),
            identifierVerified = false,
        )
    }

    @Transactional
    fun login(identifier: String, password: String, role: Role): AuthResponse {
        val user = identityService.findAccount(identifier)
            ?: throw ApiException.Unauthorized("error.auth.invalid_credentials")

        if (!passwordEncoder.matches(password, user.hashPassword)) {
            throw ApiException.Unauthorized("error.auth.invalid_credentials")
        }

        val userId = user.requireId()
        accountService.restoreRole(userId, role)

        if (role !in user.roles) {
            throw ApiException.Forbidden("error.account.role_not_present")
        }

        val identity = identityService.get(userId)
        if (!identity.verified) {
            verificationService.requestVerification(userId)
            return AuthResponse.SetUpRequired(
                identifier = identity.identifier,
                identifierType = identity.type,
                message = "error.auth.verification_code_sent".tr(),
                identifierVerified = false,
            )
        }

        val tokens = issueTokens(user, role)
        return AuthResponse.Authenticated(
            user = user.toResponse(role, profileService.getOrEmpty(userId, role), identity),
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenResponse {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ApiException.Unauthorized("error.auth.invalid_refresh_token")
        }
        val userId = jwtService.getUserIdFromToken(refreshToken).toUuid()
        val hashed = refreshToken.sha256()

        val stored = refreshTokenRepository.findByAccountIdAndHashedToken(userId, hashed)
            ?: throw ApiException.Unauthorized("error.auth.refresh_token_revoked")

        val user = accountRepository.findById(userId).orElseThrow {
            ApiException.Unauthorized("error.auth.refresh_token_revoked")
        }
        if (stored.role !in user.roles) {
            throw ApiException.Unauthorized("error.auth.refresh_token_revoked")
        }
        refreshTokenRepository.delete(stored)
        return issueTokens(user, stored.role)
    }

    fun logout(refreshToken: String, accessToken: String?) {
        if (jwtService.validateRefreshToken(refreshToken)) {
            val userId = jwtService.getUserIdFromToken(refreshToken).toUuid()
            refreshTokenRepository.deleteByAccountIdAndHashedToken(userId, refreshToken.sha256())
        }
        if (accessToken != null && jwtService.validateAccessToken(accessToken)) {
            tokenBlacklist.revoke(
                jti = jwtService.getJti(accessToken),
                expiresAt = jwtService.getExpiry(accessToken),
            )
        }
    }

    fun issueTokensForAccount(user: Account, role: Role): TokenResponse = issueTokens(user, role)

    private fun issueTokens(user: Account, role: Role): TokenResponse {
        val userId = user.requireId().toString()
        val accessToken = jwtService.generateAccessToken(userId, setOf(role))
        val refreshToken = jwtService.generateRefreshToken(userId)

        refreshTokenRepository.save(
            RefreshToken(
                account = user,
                hashedToken = refreshToken.sha256(),
                role = role,
                expiresAt = Instant.now().plus(jwtService.refreshTokenValidityMs, ChronoUnit.MILLIS),
            )
        )
        return TokenResponse(accessToken, refreshToken)
    }

    private fun Account.requireId(): UUID =
        id ?: throw IllegalStateException("Account must be persisted before its ID is used")
}
