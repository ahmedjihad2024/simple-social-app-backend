package com.example.socialapp.post.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Request body for updating an existing comment")
data class CommentUpdateRequest(
    @field:Schema(description = "Updated comment text", example = "Updated comment content...")
    @field:NotBlank(message = "{validation.comment.blank}")
    @field:Size(min = 1, max = 2000, message = "{validation.comment.size}")
    val comment: String,
)
