package com.example.socialapp.account.repository

import com.example.socialapp.account.entities.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AccountRepository : JpaRepository<Account, UUID> {

    fun findByEmail(email: String): Account?

    @Query("select a.email as email from Account a where a.id = :id")
    fun findEmailById(@Param("id") id: UUID): UserAccountEmail?
}

interface UserAccountEmail {
    val email: String?
}