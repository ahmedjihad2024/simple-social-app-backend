package com.example.socialapp.post.reaction

import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.post.reaction.dto.ReactRequest
import com.example.socialapp.post.reaction.dto.ReactResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tags(Tag(name = "User"), Tag(name = "Post"), Tag(name = "Reaction"))
@RestController
@RequestMapping("/api/user/post/{postId}/reaction")
@PreAuthorize("hasRole('USER')")
class PostLikeController(
    private val postLikeService: PostLikeService,
) {

    @PostMapping
    fun react(
        @PathVariable postId: String,
        @Valid @RequestBody body: ReactRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<ReactResponse> =
        postLikeService.react(
            postId = postId,
            accountId = currentUserId,
            request = body,
        )

    @DeleteMapping
    fun removeReaction(
        @PathVariable postId: String,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<ReactResponse> =
        postLikeService.removeReaction(
            postId = postId,
            accountId = currentUserId,
        )
}
