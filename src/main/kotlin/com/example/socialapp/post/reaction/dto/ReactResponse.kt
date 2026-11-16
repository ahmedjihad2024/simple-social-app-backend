package com.example.socialapp.post.reaction.dto

import com.example.socialapp.post.dto.ReactionsResponse
import com.example.socialapp.post.reaction.Reaction
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Reaction state for a post")
data class ReactResponse(
    @field:Schema(description = "Reaction counts breakdown on the post")
    val reactions: ReactionsResponse = ReactionsResponse(),

    @field:Schema(description = "Whether the current user has reacted to this post", example = "true")
    val likedByMe: Boolean,

    @field:Schema(description = "The current user's reaction type, or null if removed", example = "LIKE")
    val myReaction: Reaction? = null,
)

