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
import com.example.socialapp.auth.senders.VerificationCodeGenerator
import com.example.socialapp.auth.senders.email.EmailSender
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.sha256
import com.example.socialapp.security.jwt.JwtService
import com.example.socialapp.security.jwt.token_block_list.TokenBlacklist
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VerificationServiceTest {

    private val verificationCodeRepository: VerificationCodeRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val refreshTokenRepository: RefreshTokenRepository = mockk(relaxed = true)
    private val profileService: ProfileService = mockk(relaxed = true)
    private val emailSender: EmailSender = mockk(relaxed = true)
    private val verificationCodeGenerator: VerificationCodeGenerator = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val jwtService: JwtService = mockk(relaxed = true)
    private val tokenBlacklist: TokenBlacklist = mockk(relaxed = true)

    private lateinit var verificationService: VerificationService

    private val accountId = UUID.randomUUID()
    private val testAccount = Account(
        id = accountId,
        email = "current@example.com",
        hashPassword = "existingHash",
        roles = mutableSetOf(Role.USER),
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { verificationCodeRepository.save(any<VerificationCode>()) } answers { firstArg() }
        every { accountRepository.save(any<Account>()) } answers { firstArg() }
        every { tokenBlacklist.isRevoked(any()) } returns false
        every { passwordEncoder.encode(any()) } answers { "encoded_" + firstArg<String>() }

        verificationService = VerificationService(
            verificationCodeRepository = verificationCodeRepository,
            accountRepository = accountRepository,
            refreshTokenRepository = refreshTokenRepository,
            profileService = profileService,
            emailSender = emailSender,
            verificationCodeGenerator = verificationCodeGenerator,
            passwordEncoder = passwordEncoder,
            jwtService = jwtService,
            tokenBlacklist = tokenBlacklist,
        )
    }

    @Test
    fun `1 expired code - throws CodeExpired and deletes the expired record`() {
        val validCode = "123456"
        val expiredRecord = VerificationCode(
            id = UUID.randomUUID(),
            account = testAccount,
            type = VerificationType.EMAIL_VERIFICATION,
            codeHash = validCode.sha256(),
            expiresAt = Instant.now().minus(5, ChronoUnit.MINUTES), // Expired 5 minutes ago
            attempts = 0,
        )

        every { accountRepository.findByEmail(testAccount.email) } returns testAccount
        every {
            verificationCodeRepository.findByAccountIdAndType(accountId, VerificationType.EMAIL_VERIFICATION)
        } returns expiredRecord

        assertThrows<VerificationException.CodeExpired> {
            verificationService.verifyEmail(testAccount.email, validCode)
        }

        verify(exactly = 1) { verificationCodeRepository.delete(expiredRecord) }
    }

    @Test
    fun `2 wrong code - increments attempts and throws InvalidCode`() {
        val correctCode = "123456"
        val wrongCode = "999999"
        val activeRecord = VerificationCode(
            id = UUID.randomUUID(),
            account = testAccount,
            type = VerificationType.EMAIL_VERIFICATION,
            codeHash = correctCode.sha256(),
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
            attempts = 0,
        )

        every { accountRepository.findByEmail(testAccount.email) } returns testAccount
        every {
            verificationCodeRepository.findByAccountIdAndType(accountId, VerificationType.EMAIL_VERIFICATION)
        } returns activeRecord

        assertThrows<VerificationException.InvalidCode> {
            verificationService.verifyEmail(testAccount.email, wrongCode)
        }

        assertEquals(1, activeRecord.attempts)
        verify(exactly = 1) { verificationCodeRepository.save(activeRecord) }
        verify(exactly = 0) { verificationCodeRepository.delete(any()) }
    }

    @Test
    fun `3 attempts limit - 5th failed attempt deletes the code and throws MaxAttemptsExceeded`() {
        val correctCode = "123456"
        val wrongCode = "888888"
        val recordAtFourthAttempt = VerificationCode(
            id = UUID.randomUUID(),
            account = testAccount,
            type = VerificationType.EMAIL_VERIFICATION,
            codeHash = correctCode.sha256(),
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
            attempts = 4, // 4 failed attempts previously
        )

        every { accountRepository.findByEmail(testAccount.email) } returns testAccount
        every {
            verificationCodeRepository.findByAccountIdAndType(accountId, VerificationType.EMAIL_VERIFICATION)
        } returns recordAtFourthAttempt

        assertThrows<VerificationException.MaxAttemptsExceeded> {
            verificationService.verifyEmail(testAccount.email, wrongCode)
        }

        assertEquals(5, recordAtFourthAttempt.attempts)
        verify(exactly = 1) { verificationCodeRepository.delete(recordAtFourthAttempt) }
    }

    @Test
    fun `4 reuse after success - record is deleted on success and subsequent verification fails`() {
        val correctCode = "123456"
        val activeRecord = VerificationCode(
            id = UUID.randomUUID(),
            account = testAccount,
            type = VerificationType.EMAIL_VERIFICATION,
            codeHash = correctCode.sha256(),
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
            attempts = 0,
        )

        every { accountRepository.findByEmail(testAccount.email) } returns testAccount
        every {
            verificationCodeRepository.findByAccountIdAndType(accountId, VerificationType.EMAIL_VERIFICATION)
        } returns activeRecord andThen null // First call returns active record, second call returns null

        // First verification succeeds and consumes/deletes the code
        val verifiedAccount = verificationService.verifyEmail(testAccount.email, correctCode)
        assertNotNull(verifiedAccount)
        verify(exactly = 1) { verificationCodeRepository.delete(activeRecord) }
        verify(exactly = 1) { profileService.markEmailVerified(accountId, Role.USER) }

        // Second verification attempt with same code fails because single-use record was deleted
        assertThrows<VerificationException.NoPendingCode> {
            verificationService.verifyEmail(testAccount.email, correctCode)
        }
    }

    @Test
    fun `5 EMAIL_CHANGE with a different email in request - applies stored target email not request email`() {
        val correctCode = "654321"
        val legitimateTargetEmail = "verified_new@example.com"
        val fakeToken = "jwt_email_change_token"

        val changeRecord = VerificationCode(
            id = UUID.randomUUID(),
            account = testAccount,
            type = VerificationType.EMAIL_CHANGE,
            codeHash = correctCode.sha256(),
            target = legitimateTargetEmail, // Stored when code was requested
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
            attempts = 0,
        )

        every { accountRepository.findById(accountId) } returns Optional.of(testAccount)
        every {
            verificationCodeRepository.findByAccountIdAndType(accountId, VerificationType.EMAIL_CHANGE)
        } returns changeRecord
        every { jwtService.generateEmailChangeToken(accountId.toString(), legitimateTargetEmail) } returns fakeToken
        every { jwtService.validateEmailChangeToken(fakeToken) } returns true
        every { jwtService.getJti(fakeToken) } returns "fake-jti"
        every { jwtService.getExpiry(fakeToken) } returns Instant.now().plus(15, ChronoUnit.MINUTES)
        every { jwtService.getUserIdFromToken(fakeToken) } returns accountId.toString()
        every { jwtService.getNewEmailFromToken(fakeToken) } returns legitimateTargetEmail
        every { accountRepository.findByEmail(legitimateTargetEmail) } returns null // not taken

        // Step 1: verify code consumes OTP and returns token with legitimateTargetEmail
        val issuedToken = verificationService.verifyEmailChangeCode(accountId, correctCode)
        assertEquals(fakeToken, issuedToken)
        verify(exactly = 1) { verificationCodeRepository.delete(changeRecord) }

        // Step 2: confirm applies legitimateTargetEmail
        val updatedAccount = verificationService.confirmEmailChange(accountId, issuedToken)
        assertEquals(legitimateTargetEmail, updatedAccount.email)
        verify(exactly = 1) { accountRepository.save(match { it.email == legitimateTargetEmail }) }
        verify(exactly = 1) { tokenBlacklist.revoke("fake-jti", any()) }
    }

    @Test
    fun `6 password reset - separated verify code returns reset token and burns OTP`() {
        val correctCode = "777888"
        val fakeResetToken = "jwt_reset_token"
        val resetRecord = VerificationCode(
            id = UUID.randomUUID(),
            account = testAccount,
            type = VerificationType.PASSWORD_RESET,
            codeHash = correctCode.sha256(),
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
            attempts = 0,
        )

        every { accountRepository.findByEmail(testAccount.email) } returns testAccount
        every {
            verificationCodeRepository.findByAccountIdAndType(accountId, VerificationType.PASSWORD_RESET)
        } returns resetRecord
        every { jwtService.generatePasswordResetToken(accountId.toString()) } returns fakeResetToken

        val token = verificationService.verifyPasswordResetCode(testAccount.email, correctCode)

        assertEquals(fakeResetToken, token)
        verify(exactly = 1) { verificationCodeRepository.delete(resetRecord) }
    }

    @Test
    fun `7 password reset - confirm updates password, revokes reset token, and revokes refresh tokens`() {
        val fakeResetToken = "valid_jwt_reset_token"
        val newPassword = "NewStrongPassword123!"
        val jti = "test-reset-jti"
        val expiry = Instant.now().plus(10, ChronoUnit.MINUTES)

        every { jwtService.validatePasswordResetToken(fakeResetToken) } returns true
        every { jwtService.getJti(fakeResetToken) } returns jti
        every { jwtService.getExpiry(fakeResetToken) } returns expiry
        every { jwtService.getUserIdFromToken(fakeResetToken) } returns accountId.toString()
        every { accountRepository.findById(accountId) } returns Optional.of(testAccount)

        verificationService.confirmPasswordReset(fakeResetToken, newPassword)

        assertEquals("encoded_$newPassword", testAccount.hashPassword)
        verify(exactly = 1) { accountRepository.save(testAccount) }
        verify(exactly = 1) { tokenBlacklist.revoke(jti, expiry) }
        verify(exactly = 1) { refreshTokenRepository.deleteAllByAccountId(accountId) }
    }

    @Test
    fun `8 password reset - confirm with already revoked token throws ApiException`() {
        val revokedToken = "already_used_jwt_reset_token"
        val jti = "revoked-jti"

        every { jwtService.validatePasswordResetToken(revokedToken) } returns true
        every { jwtService.getJti(revokedToken) } returns jti
        every { tokenBlacklist.isRevoked(jti) } returns true // Already used/revoked

        assertThrows<ApiException.BadRequest> {
            verificationService.confirmPasswordReset(revokedToken, "NewPassword123!")
        }

        verify(exactly = 0) { accountRepository.save(any()) }
        verify(exactly = 0) { refreshTokenRepository.deleteAllByAccountId(any()) }
    }
}
