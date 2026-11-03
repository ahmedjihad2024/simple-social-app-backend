package com.example.socialapp.identity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AuthIdentityRepository : JpaRepository<AuthIdentity, UUID> {

    @Query("select i from AuthIdentity i join fetch i.account where i.type = :type and i.identifier = :identifier")
    fun findWithAccount(
        @Param("type") type: IdentifierType,
        @Param("identifier") identifier: String,
    ): AuthIdentity?

    fun existsByTypeAndIdentifier(type: IdentifierType, identifier: String): Boolean

    fun findByAccountIdAndType(accountId: UUID, type: IdentifierType): AuthIdentity?
}
