package com.example.socialapp.post.reaction

import com.example.socialapp.account.entities.Account
import com.example.socialapp.post.entites.Post
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class PostLikeId(
    @Column(name = "account_id") val accountId: UUID,
    @Column(name = "post_id") val postId: UUID,
) : Serializable

@Entity
@Table(
    name = "posts_likes", indexes = [
        Index(name = "idx_posts_likes_post_id", columnList = "post_id"),
    ]
)
class PostLike(

    @EmbeddedId
    val id: PostLikeId,

    @MapsId("accountId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account,

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    var type: Reaction,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)