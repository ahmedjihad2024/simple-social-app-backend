package com.example.socialapp.account.repository

import com.example.socialapp.account.entities.Account
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

// Email / phone lookups live in AuthIdentityRepository (auth.identity) — Account is identifier-agnostic.
interface AccountRepository : JpaRepository<Account, UUID>
