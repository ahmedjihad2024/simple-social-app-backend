package com.example.socialapp.auth.repository

import com.example.socialapp.auth.entities.VerificationCode
import com.example.socialapp.auth.entities.VerificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface VerificationCodeRepository : JpaRepository<VerificationCode, UUID> {

    fun findByAccountIdAndType(accountId: UUID, type: VerificationType): VerificationCode?

    @Modifying
    @Query("delete from VerificationCode c where c.account.id = :accountId and c.type = :type")
    fun deleteByAccountIdAndType(
        @Param("accountId") accountId: UUID,
        @Param("type") type: VerificationType,
    ): Int

    @Modifying
    @Query("delete from VerificationCode c where c.account.id = :accountId")
    fun deleteByAccountId(@Param("accountId") accountId: UUID): Int

    @Modifying
    @Query("delete from VerificationCode c where c.expiresAt < :now")
    fun deleteExpired(@Param("now") now: Instant): Int
}
