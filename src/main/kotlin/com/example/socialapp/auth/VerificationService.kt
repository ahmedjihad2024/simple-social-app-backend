package com.example.socialapp.auth

import com.example.socialapp.account.ProfileService
import com.example.socialapp.account.entities.Account
import com.example.socialapp.account.enums.Role
import com.example.socialapp.account.repository.AccountRepository
import com.example.socialapp.auth.entities.VerificationCode
import com.example.socialapp.auth.entities.VerificationType
import com.example.socialapp.auth.exception.VerificationException
import com.example.socialapp.auth.repository.RefreshTokenRepository
import com.example.socialapp.auth.repository.VerificationCodeRepository
import com.example.socialapp.auth.senders.VERIFICATION_CODE_TTL_MINUTES
import com.example.socialapp.auth.senders.VerificationCodeGenerator
import com.example.socialapp.auth.senders.email.EmailSender
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.sha256
import com.example.socialapp.common.extentions.toUuid
import com.example.socialapp.common.extentions.tr
import com.example.socialapp.security.jwt.JwtService
import com.example.socialapp.security.jwt.token_block_list.TokenBlacklist
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class VerificationService(
    private val verificationCodeRepository: VerificationCodeRepository,
    private val accountRepository: AccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val profileService: ProfileService,
    private val emailSender: EmailSender,
    private val verificationCodeGenerator: VerificationCodeGenerator,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val tokenBlacklist: TokenBlacklist,
) {

    @Transactional
    fun requestEmailVerification(email: String): String {
        val account = accountRepository.findByEmail(email)
            ?: throw ApiException.NotFound("error.account.not_found")
        return sendEmailVerification(account)
    }

    @Transactional
    fun requestEmailVerification(accountId: UUID): String {
        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }
        return sendEmailVerification(account)
    }

    private fun sendEmailVerification(account: Account): String {
        val code = verificationCodeGenerator.generate()
        saveOrReplaceCode(account, VerificationType.EMAIL_VERIFICATION, code)
        emailSender.sendVerificationCode(account.email, code)
        return "error.auth.verification_code_sent".tr()
    }

    @Transactional
    fun verifyEmail(email: String, code: String): Account {
        val account = accountRepository.findByEmail(email)
            ?: throw ApiException.NotFound("error.account.not_found")
        verifyAndConsumeCode(account, VerificationType.EMAIL_VERIFICATION, code)
        profileService.markEmailVerified(account.requireId(), Role.USER)
        return account
    }

    @Transactional
    fun requestEmailChange(accountId: UUID, newEmail: String): String {
        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }

        if (newEmail.equals(account.email, ignoreCase = true)) {
            throw ApiException.BadRequest("error.account.email_same_as_current")
        }

        val taken = accountRepository.findByEmail(newEmail)
        if (taken != null && taken.id != account.id) {
            throw ApiException.Conflict("error.auth.email_already_exists")
        }

        val code = verificationCodeGenerator.generate()
        saveOrReplaceCode(account, VerificationType.EMAIL_CHANGE, code, target = newEmail)
        emailSender.sendVerificationCode(newEmail, code)
        return "error.account.email_change_code_sent".tr()
    }

    /**
     * Step 1 of Email Change: Verifies the 6-digit code sent to the new email,
     * burns the OTP, and returns a short-lived email change token.
     */
    @Transactional
    fun verifyEmailChangeCode(accountId: UUID, code: String): String {
        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }

        val record = verifyAndConsumeCode(account, VerificationType.EMAIL_CHANGE, code)
        val targetEmail = record.target ?: throw VerificationException.NoPendingCode()

        return jwtService.generateEmailChangeToken(account.requireId().toString(), targetEmail)
    }

    /**
     * Step 2 of Email Change: Validates the short-lived token, checks email uniqueness,
     * updates the account email, and invalidates the token.
     */
    @Transactional
    fun confirmEmailChange(accountId: UUID, token: String): Account {
        if (!jwtService.validateEmailChangeToken(token)) {
            throw ApiException.BadRequest("error.auth.invalid_token")
        }
        val jti = jwtService.getJti(token)
        if (tokenBlacklist.isRevoked(jti)) {
            throw ApiException.BadRequest("error.auth.invalid_token")
        }
        val tokenUserId = jwtService.getUserIdFromToken(token).toUuid()
        if (tokenUserId != accountId) {
            throw ApiException.Forbidden("error.auth.invalid_token")
        }
        val targetEmail = jwtService.getNewEmailFromToken(token)
            ?: throw ApiException.BadRequest("error.auth.invalid_token")

        val taken = accountRepository.findByEmail(targetEmail)
        if (taken != null && taken.id != accountId) {
            throw ApiException.Conflict("error.auth.email_already_exists")
        }

        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }

        account.email = targetEmail
        accountRepository.save(account)

        tokenBlacklist.revoke(jti, jwtService.getExpiry(token))
        return account
    }

    @Transactional
    fun verifyEmailChange(accountId: UUID, code: String, ignoredRequestBodyEmail: String? = null): Account {
        val token = verifyEmailChangeCode(accountId, code)
        return confirmEmailChange(accountId, token)
    }

    @Transactional
    fun requestPasswordReset(email: String): String {
        val account = accountRepository.findByEmail(email)
        if (account != null) {
            val code = verificationCodeGenerator.generate()
            saveOrReplaceCode(account, VerificationType.PASSWORD_RESET, code)
            emailSender.sendVerificationCode(account.email, code)
        }
        // Always return the exact same response whether the email exists or not (anti-enumeration)
        return "error.auth.verification_code_sent".tr()
    }

    /**
     * Step 1 of Password Reset: Verifies the 6-digit code sent to the email,
     * burns the OTP, and returns a short-lived reset token.
     */
    @Transactional
    fun verifyPasswordResetCode(email: String, code: String): String {
        val account = accountRepository.findByEmail(email)
            ?: throw VerificationException.InvalidCode()

        verifyAndConsumeCode(account, VerificationType.PASSWORD_RESET, code)
        return jwtService.generatePasswordResetToken(account.requireId().toString())
    }

    /**
     * Step 2 of Password Reset: Validates the short-lived reset token, updates the password,
     * invalidates the token, and revokes all refresh tokens.
     */
    @Transactional
    fun confirmPasswordReset(resetToken: String, newPassword: String) {
        if (!jwtService.validatePasswordResetToken(resetToken)) {
            throw ApiException.BadRequest("error.auth.invalid_token")
        }
        val jti = jwtService.getJti(resetToken)
        if (tokenBlacklist.isRevoked(jti)) {
            throw ApiException.BadRequest("error.auth.invalid_token")
        }
        val accountId = jwtService.getUserIdFromToken(resetToken).toUuid()
        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }

        account.hashPassword = passwordEncoder.encode(newPassword)!!
        accountRepository.save(account)

        tokenBlacklist.revoke(jti, jwtService.getExpiry(resetToken))

        // After a successful reset, revoke all refresh tokens of the account
        refreshTokenRepository.deleteAllByAccountId(account.requireId())
    }

    @Transactional
    fun confirmPasswordReset(email: String, code: String, newPassword: String) {
        val resetToken = verifyPasswordResetCode(email, code)
        confirmPasswordReset(resetToken, newPassword)
    }

    private fun verifyAndConsumeCode(
        account: Account,
        type: VerificationType,
        inputCode: String,
    ): VerificationCode {
        val accountId = account.requireId()
        val record = verificationCodeRepository.findByAccountIdAndType(accountId, type)
            ?: throw VerificationException.NoPendingCode()

        if (record.expiresAt.isBefore(Instant.now())) {
            verificationCodeRepository.delete(record)
            throw VerificationException.CodeExpired()
        }

        if (!constantTimeEquals(inputCode.sha256(), record.codeHash)) {
            record.attempts += 1
            if (record.attempts >= 5) {
                verificationCodeRepository.delete(record)
                throw VerificationException.MaxAttemptsExceeded()
            } else {
                verificationCodeRepository.save(record)
                throw VerificationException.InvalidCode()
            }
        }

        // Single use: delete the record after successful verification
        verificationCodeRepository.delete(record)
        return record
    }

    private fun saveOrReplaceCode(
        account: Account,
        type: VerificationType,
        plainCode: String,
        target: String? = null,
    ): VerificationCode {
        val accountId = account.requireId()
        val existing = verificationCodeRepository.findByAccountIdAndType(accountId, type)
        val entity = if (existing != null) {
            existing.codeHash = plainCode.sha256()
            existing.target = target
            existing.expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES)
            existing.attempts = 0
            existing
        } else {
            VerificationCode(
                account = account,
                type = type,
                codeHash = plainCode.sha256(),
                target = target,
                expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES),
                attempts = 0,
            )
        }
        return verificationCodeRepository.save(entity)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(StandardCharsets.UTF_8)
        val bBytes = b.toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    private fun Account.requireId(): UUID =
        id ?: throw IllegalStateException("Account must be persisted before its ID is used")
}
