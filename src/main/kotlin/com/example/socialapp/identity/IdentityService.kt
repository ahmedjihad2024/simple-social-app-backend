package com.example.socialapp.identity

import com.example.socialapp.account.entities.Account
import com.example.socialapp.common.exception.ApiException
import org.springframework.stereotype.Service
import java.util.UUID

/** Everything the rest of the app needs to know about an account's email / phone. */
@Service
class IdentityService(
    private val identities: AuthIdentityRepository,
    properties: AuthProperties,
) {
    /** The identifier type this deployment signs in with (`app.auth.identifier`). */
    val type: IdentifierType = properties.identifier

    fun normalize(raw: String): String = IdentifierRules.normalize(type, raw)

    /** The account that owns this identifier, or null. Accepts raw user input. */
    fun findAccount(rawIdentifier: String): Account? =
        identities.findWithAccount(type, normalize(rawIdentifier))?.account

    /** [identifier] must already be normalised. */
    fun exists(identifier: String): Boolean = identities.existsByTypeAndIdentifier(type, identifier)

    /** True if [identifier] (already normalised) belongs to some account other than [accountId]. */
    fun isTakenByOther(identifier: String, accountId: UUID): Boolean {
        val other = identities.findWithAccount(type, identifier) ?: return false
        return other.account.id != accountId
    }

    /** [identifier] must already be normalised. New identities start unverified. */
    fun create(account: Account, identifier: String): AuthIdentity =
        identities.save(AuthIdentity(account = account, type = type, identifier = identifier))

    fun get(accountId: UUID): AuthIdentity =
        identities.findByAccountIdAndType(accountId, type)
            ?: throw ApiException.NotFound("error.account.not_found")

    fun isVerified(accountId: UUID): Boolean =
        identities.findByAccountIdAndType(accountId, type)?.verified == true

    fun markVerified(accountId: UUID) {
        val identity = get(accountId)
        identity.verified = true
        identities.save(identity)
    }

    /** Replaces the identifier after the user proved they own the new one, so it is verified. */
    fun change(accountId: UUID, newIdentifier: String) {
        val identity = get(accountId)
        identity.identifier = newIdentifier
        identity.verified = true
        identities.save(identity)
    }
}
