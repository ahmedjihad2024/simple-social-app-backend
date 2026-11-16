package com.example.socialapp.post.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Comment author details")
data class CommentAccountResponse(
    @field:Schema(description = "Account ID of the author")
    val id: UUID,

    @field:Schema(description = "Full name of the author", example = "John Doe")
    val name: String? = null,

    @field:Schema(description = "URL to the author's avatar image", example = "http://localhost:8080/media/avatars/avatar.png")
    val imageUrl: String? = null,
)

@Schema(description = "Comment details returned by the API")
data class CommentResponse(
    @field:Schema(description = "Unique identifier of the comment")
    val id: UUID,

//    @field:Schema(description = "Post ID this comment belongs to")
//    val postId: UUID,

    @field:Schema(description = "Account details of the author")
    val account: CommentAccountResponse,

//    @field:Schema(description = "Parent comment ID, if this is a reply to another comment")
//    val parentId: UUID? = null,

    @field:Schema(description = "Comment text")
    val comment: String,

    @field:Schema(description = "Whether this comment has replies", example = "false")
    val hasReplies: Boolean = false,

    @field:Schema(description = "Creation timestamp")
    val createdAt: Instant,
)
