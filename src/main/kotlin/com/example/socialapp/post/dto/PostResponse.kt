package com.example.socialapp.post.dto

import com.example.socialapp.post.reaction.Reaction
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Reaction counts breakdown for a post")
data class ReactionsResponse(
    @field:Schema(description = "Total number of reactions", example = "42")
    val total: Long = 0,

    @field:Schema(description = "Number of LIKE reactions", example = "20")
    val like: Long = 0,

    @field:Schema(description = "Number of LOVE reactions", example = "10")
    val love: Long = 0,

    @field:Schema(description = "Number of HAHA reactions", example = "5")
    val haha: Long = 0,

    @field:Schema(description = "Number of WOW reactions", example = "3")
    val wow: Long = 0,

    @field:Schema(description = "Number of SAD reactions", example = "2")
    val sad: Long = 0,

    @field:Schema(description = "Number of ANGRY reactions", example = "2")
    val angry: Long = 0,
)

@Schema(description = "Post author details")
data class PostAccountResponse(
    @field:Schema(description = "Account ID of the author")
    val id: UUID,

    @field:Schema(description = "Full name of the author", example = "John Doe")
    val name: String? = null,

    @field:Schema(description = "URL to the author's avatar image", example = "http://localhost:8080/media/avatars/avatar.png")
    val imageUrl: String? = null,
)

@Schema(description = "Post details response")
data class PostResponse(
    @field:Schema(description = "Unique ID of the post")
    val id: UUID,

    @field:Schema(description = "Account details of the post author")
    val account: PostAccountResponse,

    @field:Schema(description = "Title of the post", example = "Exciting news about our project")
    val title: String,

    @field:Schema(description = "Body content of the post", example = "This is the content of the post...")
    val body: String,

    @field:Schema(description = "List of image URLs attached to the post")
    val images: List<String> = emptyList(),

    @field:Schema(description = "Total number of comments on the post", example = "5")
    val commentsCount: Long = 0,

    @field:Schema(description = "Reaction counts breakdown on the post")
    val reactions: ReactionsResponse = ReactionsResponse(),

    @field:Schema(description = "Whether the current user has reacted to this post", example = "true")
    val likedByMe: Boolean = false,

    @field:Schema(description = "The current user's reaction type, if any", example = "LIKE")
    val myReaction: Reaction? = null,

    @field:Schema(description = "Timestamp when the post was created")
    val createdAt: Instant,

    @field:Schema(description = "Timestamp when the post was last updated, if any")
    val updatedAt: Instant? = null,
)
