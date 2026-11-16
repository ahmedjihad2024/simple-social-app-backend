package com.example.socialapp.post.entites

import com.example.socialapp.account.entities.Account
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_posts_account_id",columnList = "account_id"),
    ]
)
class Post(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account,

    var title: String,

    var body: String,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("position ASC")
    val images: MutableList<PostImage> = mutableListOf(),


    @UpdateTimestamp
    var updatedAt: Instant? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)