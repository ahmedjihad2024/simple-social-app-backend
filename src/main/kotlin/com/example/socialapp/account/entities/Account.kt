package com.example.socialapp.account.entities

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Index
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import com.example.socialapp.account.enums.Role

@Entity
@Table(
    name = "accounts"
)
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(
        name = "hash_password",
        nullable = false,
        length = 100
    )
    var hashPassword: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "account_roles", joinColumns = [JoinColumn(name = "account_id")])
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val roles: MutableSet<Role> = mutableSetOf(Role.USER),

    @Version
    val version: Long? = null,
) {
    companion object {
        /** Grace period a soft-deleted persona is kept before it is purged. */
        const val DELETED_RETENTION_DAYS = 30L

        /** Name shown to other users in place of a deleted account. */
        const val DELETED_DISPLAY_NAME = "Deleted account"
    }
}
