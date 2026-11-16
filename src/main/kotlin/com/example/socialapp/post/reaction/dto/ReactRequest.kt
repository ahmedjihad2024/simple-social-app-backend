package com.example.socialapp.post.reaction.dto

import com.example.socialapp.post.reaction.Reaction
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request body for adding or updating a reaction on a post")
data class ReactRequest(
    @field:Schema(description = "Reaction type", example = "LIKE")
    @field:NotNull(message = "{validation.cannot_be_blank}")
    val type: Reaction,
)
