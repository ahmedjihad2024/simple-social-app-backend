package com.example.socialapp.post.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "Request body for adding a comment to a post or replying to a comment")
data class CommentCreateRequest(
    @field:Schema(description = "Comment text", example = "Great post!")
    @field:NotBlank(message = "{validation.comment.blank}")
    @field:Size(min = 1, max = 2000, message = "{validation.comment.size}")
    val comment: String,

    @field:Schema(description = "Optional parent comment ID if replying to an existing comment")
    val parentId: UUID? = null,
)
