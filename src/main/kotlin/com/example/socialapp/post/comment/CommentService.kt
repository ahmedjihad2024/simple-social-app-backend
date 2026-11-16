package com.example.socialapp.post.comment

import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.toUuid
import com.example.socialapp.post.PostRepository
import com.example.socialapp.post.comment.dto.CommentCreateRequest
import com.example.socialapp.post.comment.dto.CommentResponse
import com.example.socialapp.post.comment.dto.CommentUpdateRequest
import com.example.socialapp.post.comment.mapper.toResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
) {

    @Transactional
    fun createComment(
        postId: String,
        accountId: String,
        request: CommentCreateRequest,
    ): ApiResponse<CommentResponse> {
        val postUuid = postId.toUuid()
        val accountUuid = accountId.toUuid()

        // 1. Verify that the post exists
        if (!postRepository.existsById(postUuid)) {
            throw ApiException.NotFound("error.post.not_found")
        }

        // 2. If this is a reply (parentId != null), verify parent comment exists and belongs to this post
        val parentUuid = request.parentId
        if (parentUuid != null) {
            val parentPostId = commentRepository.findPostIdById(parentUuid)
                ?: throw ApiException.NotFound("error.comment.not_found")

            if (parentPostId != postUuid) {
                throw ApiException.BadRequest("error.comment.invalid_parent")
            }
        }

        // 3. Insert the new comment
        val created = commentRepository.createComment(
            accountId = accountUuid,
            postId = postUuid,
            parentId = parentUuid,
            comment = request.comment.trim(),
        )

        return ApiResponse.ok(created.toResponse())
    }

    @Transactional(readOnly = true)
    fun getComments(
        postId: String,
        parentId: String?,
        pageable: Pageable,
    ): ApiResponse<List<CommentResponse>> {
        val postUuid = postId.toUuid()

        // 1. Verify post exists
        if (!postRepository.existsById(postUuid)) {
            throw ApiException.NotFound("error.post.not_found")
        }

        // 2. If parentId is provided, validate it exists and belongs to this post
        val parentUuid = parentId?.toUuid()
        if (parentUuid != null) {
            val parentPostId = commentRepository.findPostIdById(parentUuid)
                ?: throw ApiException.NotFound("error.comment.not_found")

            if (parentPostId != postUuid) {
                throw ApiException.BadRequest("error.comment.invalid_parent")
            }
        }

        // 3. Query comments or replies
        val page = commentRepository.findComments(
            postId = postUuid,
            parentId = parentUuid?.toString(),
            pageable = pageable,
        )

        return ApiResponse.paged(page.map { it.toResponse() })
    }

    @Transactional
    fun updateComment(
        postId: String,
        commentId: String,
        accountId: String,
        request: CommentUpdateRequest,
    ): ApiResponse<CommentResponse> {
        val postUuid = postId.toUuid()
        val commentUuid = commentId.toUuid()
        val accountUuid = accountId.toUuid()

        val updated = commentRepository.updateComment(
            commentId = commentUuid,
            postId = postUuid,
            accountId = accountUuid,
            comment = request.comment.trim(),
        ) ?: run {
            // Check if comment exists to differentiate 404 (not found) vs 403 (cannot modify / not owner)
            if (commentRepository.existsById(commentUuid)) {
                throw ApiException.Forbidden("error.comment.cannot_modify")
            } else {
                throw ApiException.NotFound("error.comment.not_found")
            }
        }

        return ApiResponse.ok(updated.toResponse())
    }
}
