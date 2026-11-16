package com.example.socialapp.post.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request body for updating a post (all fields are optional)")
data class PostUpdateRequest(
    @field:Schema(description = "Post title", example = "Updated post title")
    @field:Size(min = 3, max = 255, message = "{validation.post.title.size}")
    val title: String? = null,

    @field:Schema(description = "Post body content", example = "Updated body content...")
    @field:Size(min = 1, max = 10000, message = "{validation.post.body.size}")
    val body: String? = null,
)
