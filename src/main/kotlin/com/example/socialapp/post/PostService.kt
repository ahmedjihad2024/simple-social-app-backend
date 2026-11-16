package com.example.socialapp.post

import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.common.exception.ApiException
import com.example.socialapp.common.extentions.toUuid
import com.example.socialapp.common.storage.FileCategory
import com.example.socialapp.common.storage.FileStorageService
import com.example.socialapp.post.dto.PostCreateRequest
import com.example.socialapp.post.dto.PostResponse
import com.example.socialapp.post.mapper.toResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PostService(
    private val storageService: FileStorageService,
    private val postRepository: PostRepository
) {

    private companion object {
        val NO_VIEWER = UUID(0L, 0L)
    }

    fun createPost(
        accountId: String,
        request: PostCreateRequest
    ): ApiResponse<PostResponse> {

        val userId = accountId.toUuid()
        val stored = mutableListOf<String>()

        try {
            request.images?.forEach { image ->
                stored += storageService.store(FileCategory.IMAGES, image)
            }

            val result = postRepository.createPost(
                accountId = userId,
                title = request.title,
                body = request.body,
                images = stored.toTypedArray(),
            )

            return ApiResponse.ok(result.toResponse())
        } catch (e: Exception) {
            stored.forEach { runCatching { storageService.delete(FileCategory.IMAGES,it) } }
            throw e
        }
    }

    fun getPostDetails(
        postId: String,
        viewerId: String?,
    ): ApiResponse<PostResponse> {
        val postUuid = postId.toUuid()
        val viewerUuid = viewerId?.toUuid() ?: NO_VIEWER

        val post = postRepository.postDetails(
            postId = postUuid,
            viewerId = viewerUuid,
        ) ?: throw ApiException.NotFound("error.post.not_found")

        return ApiResponse.ok(post.toResponse())
    }

    @Transactional(readOnly = true)
    fun getPosts(
        viewerId: String?,
        pageable: Pageable,
    ): ApiResponse<List<PostResponse>> {
        val viewerUuid = viewerId?.toUuid() ?: NO_VIEWER
        val page = postRepository.posts(
            viewerId = viewerUuid,
            pageable = pageable,
        )
        return ApiResponse.paged(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun getMyPosts(
        currentUserId: String,
        pageable: Pageable,
    ): ApiResponse<List<PostResponse>> {
        val userUuid = currentUserId.toUuid()
        val page = postRepository.posts(
            viewerId = userUuid,
            accountId = userUuid,
            pageable = pageable,
        )
        return ApiResponse.paged(page.map { it.toResponse() })
    }

    @Transactional
    fun updatePost(
        postId: String,
        accountId: String,
        title: String?,
        body: String?,
    ): ApiResponse<PostResponse> {
        val postUuid = postId.toUuid()
        val accountUuid = accountId.toUuid()

        val updated = postRepository.updatePost(
            postId = postUuid,
            accountId = accountUuid,
            title = title,
            body = body,
        ) ?: run {
            // Check if post exists to differentiate 404 (not found) vs 403 (cannot modify / not owner)
            if (postRepository.existsById(postUuid)) {
                throw ApiException.Forbidden("error.post.cannot_modify")
            } else {
                throw ApiException.NotFound("error.post.not_found")
            }
        }

        return ApiResponse.ok(updated.toResponse())
    }
}

