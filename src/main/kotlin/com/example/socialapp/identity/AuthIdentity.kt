package com.example.socialapp.identity

import com.example.socialapp.account.entities.Account
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant
import java.util.UUID

/**
 * How an account proves who it is: one row per (account, type). The account itself knows nothing
 * about email or phone — swap or add a type and nothing outside this package has to change.
 */
@Entity
@Table(
    name = "auth_identities",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_auth_identities_type_identifier", columnNames = ["type", "identifier"]),
        UniqueConstraint(name = "uq_auth_identities_account_type", columnNames = ["account_id", "type"]),
    ],
)
class AuthIdentity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val type: IdentifierType,

    /** Normalised email address or E.164 phone number. */
    @Column(name = "identifier", nullable = false, length = 300)
    var identifier: String,

    @Column(nullable = false)
    var verified: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
