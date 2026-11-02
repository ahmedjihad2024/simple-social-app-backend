package com.example.socialapp.auth.entities

import com.example.socialapp.account.entities.Account
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "verification_codes",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_verification_codes_account_type", columnNames = ["account_id", "type"])
    ],
    indexes = [
        Index(name = "idx_verification_codes_expires_at", columnList = "expires_at")
    ]
)
class VerificationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: VerificationType,

    @Column(name = "code_hash", nullable = false, length = 100)
    var codeHash: String,

    @Column(length = 300)
    var target: String? = null,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
