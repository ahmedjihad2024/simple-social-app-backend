package com.example.socialapp.post.comment

import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.post.comment.dto.CommentCreateRequest
import com.example.socialapp.post.comment.dto.CommentResponse
import com.example.socialapp.post.comment.dto.CommentUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tags(Tag(name = "User"), Tag(name = "Post"), Tag(name = "Comment"))
@RestController
@RequestMapping("/api/user/post/{postId}/comment")
class CommentController(
    private val commentService: CommentService,
) {

    @GetMapping
    @Operation(summary = "Get comments of a post, or replies to a specific comment (paginated)")
    fun getComments(
        @PathVariable postId: String,
        @RequestParam(required = false) parentId: String?,
        pageable: Pageable,
    ): ApiResponse<List<CommentResponse>> =
        commentService.getComments(
            postId = postId,
            parentId = parentId,
            pageable = pageable,
        )

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a comment or reply on a post")
    fun createComment(
        @PathVariable postId: String,
        @Valid @RequestBody body: CommentCreateRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<CommentResponse> =
        commentService.createComment(
            postId = postId,
            accountId = currentUserId,
            request = body,
        )

    @PatchMapping("/{commentId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Edit an existing comment")
    fun updateComment(
        @PathVariable postId: String,
        @PathVariable commentId: String,
        @Valid @RequestBody body: CommentUpdateRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<CommentResponse> =
        commentService.updateComment(
            postId = postId,
            commentId = commentId,
            accountId = currentUserId,
            request = body,
        )
}
