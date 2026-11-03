package com.example.socialapp.auth

import com.example.socialapp.account.entities.Account
import com.example.socialapp.account.repository.AccountRepository
import com.example.socialapp.auth.entities.VerificationCode
import com.example.socialapp.auth.entities.VerificationType
import com.example.socialapp.auth.exception.VerificationException
import com.example.socialapp.identity.IdentifierType
import com.example.socialapp.identity.IdentityService
import com.example.socialapp.auth.repository.RefreshTokenRepository
import com.example.socialapp.auth.repository.VerificationCodeRepository
import com.example.socialapp.auth.senders.OtpSender
import com.example.socialapp.auth.senders.VERIFICATION_CODE_TTL_MINUTES
import com.example.socialapp.auth.senders.VerificationCodeGenerator
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

/**
 * Verification codes, password reset and identifier (email / phone) change.
 * Works on a generic "identifier" — whether that is an email or a phone number is decided by
 * `app.auth.identifier`, and the matching [OtpSender] delivers the code.
 */
@Service
class VerificationService(
    private val verificationCodeRepository: VerificationCodeRepository,
    private val accountRepository: AccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val identityService: IdentityService,
    senders: List<OtpSender>,
    private val verificationCodeGenerator: VerificationCodeGenerator,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val tokenBlacklist: TokenBlacklist,
) {
    private val sendersByType: Map<IdentifierType, OtpSender> = senders.associateBy { it.type }

    init {
        // Fail at startup, not on the first registration, if the chosen channel has no sender bean.
        check(identityService.type in sendersByType) {
            "app.auth.identifier=${identityService.type} but no OtpSender exists for it. " +
                "Check app.mail.backend / app.sms.backend and provide a sender implementation."
        }
    }

    private fun deliver(target: String, code: String) =
        sendersByType.getValue(identityService.type).sendVerificationCode(target, code)

    // ---------------------------------------------------------------- identifier verification

    @Transactional
    fun requestVerification(identifier: String): String {
        val account = identityService.findAccount(identifier)
            ?: throw ApiException.NotFound("error.account.not_found")
        return sendVerification(account)
    }

    @Transactional
    fun requestVerification(accountId: UUID): String {
        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }
        return sendVerification(account)
    }

    private fun sendVerification(account: Account): String {
        val identity = identityService.get(account.requireId())
        val code = verificationCodeGenerator.generate()
        saveOrReplaceCode(account, VerificationType.IDENTIFIER_VERIFICATION, code)
        deliver(identity.identifier, code)
        return "error.auth.verification_code_sent".tr()
    }

    @Transactional
    fun verify(identifier: String, code: String): Account {
        val account = identityService.findAccount(identifier)
            ?: throw ApiException.NotFound("error.account.not_found")
        verifyAndConsumeCode(account, VerificationType.IDENTIFIER_VERIFICATION, code)
        identityService.markVerified(account.requireId())
        return account
    }

    // ---------------------------------------------------------------- identifier change

    @Transactional
    fun requestIdentifierChange(accountId: UUID, newIdentifier: String): String {
        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }

        val target = identityService.normalize(newIdentifier)
        if (target == identityService.get(accountId).identifier) {
            throw ApiException.BadRequest("error.account.identifier_same_as_current")
        }
        if (identityService.isTakenByOther(target, accountId)) {
            throw ApiException.Conflict("error.auth.identifier_already_exists")
        }

        val code = verificationCodeGenerator.generate()
        saveOrReplaceCode(account, VerificationType.IDENTIFIER_CHANGE, code, target = target)
        deliver(target, code)
        return "error.account.identifier_change_code_sent".tr()
    }

    /**
     * Step 1 of identifier change: verifies the 6-digit code sent to the new email / phone,
     * burns the OTP, and returns a short-lived identifier change token.
     */
    @Transactional
    fun verifyIdentifierChangeCode(accountId: UUID, code: String): Account {
        val account = accountRepository.findById(accountId)
            .orElseThrow { ApiException.NotFound("error.account.not_found") }

        val record = verifyAndConsumeCode(account, VerificationType.IDENTIFIER_CHANGE, code)
        val target = record.target ?: throw VerificationException.NoPendingCode()

        if (identityService.isTakenByOther(target, accountId)) {
            throw ApiException.Conflict("error.auth.identifier_already_exists")
        }

        identityService.change(accountId, target)

        return account
    }

    // ---------------------------------------------------------------- password reset

    @Transactional
    fun requestPasswordReset(identifier: String): String {
        val account = identityService.findAccount(identifier)
        if (account != null) {
            val identity = identityService.get(account.requireId())
            val code = verificationCodeGenerator.generate()
            saveOrReplaceCode(account, VerificationType.PASSWORD_RESET, code)
            deliver(identity.identifier, code)
        }
        // Always return the exact same response whether the identifier exists or not (anti-enumeration)
        return "error.auth.verification_code_sent".tr()
    }

    /**
     * Step 1 of Password Reset: Verifies the 6-digit code sent to the user,
     * burns the OTP, and returns a short-lived reset token.
     */
    @Transactional
    fun verifyPasswordResetCode(identifier: String, code: String): String {
        val account = identityService.findAccount(identifier)
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
    fun confirmPasswordReset(identifier: String, code: String, newPassword: String) {
        val resetToken = verifyPasswordResetCode(identifier, code)
        confirmPasswordReset(resetToken, newPassword)
    }

    // ---------------------------------------------------------------- internals

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
