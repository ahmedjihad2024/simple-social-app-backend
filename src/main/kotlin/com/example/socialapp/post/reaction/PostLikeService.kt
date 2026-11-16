package com.example.socialapp.post.reaction

import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.toUuid
import com.example.socialapp.post.PostRepository
import com.example.socialapp.post.dto.ReactionsResponse
import com.example.socialapp.post.reaction.dto.ReactRequest
import com.example.socialapp.post.reaction.dto.ReactResponse
import com.example.socialapp.post.mapper.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLikeService(
    private val postLikeRepository: PostLikeRepository,
    private val postRepository: PostRepository,
) {

    @Transactional
    fun react(
        postId: String,
        accountId: String,
        request: ReactRequest,
    ): ApiResponse<ReactResponse> {
        val postUuid = postId.toUuid()
        val accountUuid = accountId.toUuid()

        if (!postRepository.existsById(postUuid)) {
            throw ApiException.NotFound("error.post.not_found")
        }

        postLikeRepository.react(
            accountId = accountUuid,
            postId = postUuid,
            type = request.type.name,
        )

        val breakdown = postLikeRepository.getReactions(postId = postUuid, viewerId = accountUuid)

        return ApiResponse.ok(breakdown.toResponse())
    }

    @Transactional
    fun removeReaction(
        postId: String,
        accountId: String,
    ): ApiResponse<ReactResponse> {
        val postUuid = postId.toUuid()
        val accountUuid = accountId.toUuid()

        if (!postRepository.existsById(postUuid)) {
            throw ApiException.NotFound("error.post.not_found")
        }

        val likeId = PostLikeId(accountId = accountUuid, postId = postUuid)
        if (postLikeRepository.existsById(likeId)) {
            postLikeRepository.deleteById(likeId)
        }

        val breakdown = postLikeRepository.getReactions(postId = postUuid, viewerId = accountUuid)

        return ApiResponse.ok(breakdown.toResponse())
    }
}


