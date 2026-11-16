package com.example.socialapp.post

import com.example.socialapp.common.dto.ApiResponse
import com.example.socialapp.post.dto.PostCreateRequest
import com.example.socialapp.post.dto.PostResponse
import com.example.socialapp.post.dto.PostUpdateRequest
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tags(Tag(name = "User"), Tag(name = "Post"))
@RestController
@RequestMapping("/api/user/post")
class PostController(
    private val postService: PostService,
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPost(
        @Valid @ModelAttribute body: PostCreateRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<PostResponse> {
        return postService.createPost(
            accountId = currentUserId,
            request = body
        )
    }

    @GetMapping
    fun getPosts(
        @AuthenticationPrincipal currentUserId: String?,
        pageable: Pageable,
    ): ApiResponse<List<PostResponse>> =
        postService.getPosts(
            viewerId = currentUserId?.takeIf { it != "anonymousUser" },
            pageable = pageable,
        )

    @GetMapping("/me")
    fun getMyPosts(
        @AuthenticationPrincipal currentUserId: String,
        pageable: Pageable,
    ): ApiResponse<List<PostResponse>> =
        postService.getMyPosts(
            currentUserId = currentUserId,
            pageable = pageable,
        )


    @GetMapping("/{id}")
    fun getPostDetails(
        @PathVariable id: String,
        @AuthenticationPrincipal currentUserId: String?,
    ): ApiResponse<PostResponse> =
        postService.getPostDetails(
            postId = id,
            viewerId = currentUserId?.takeIf { it != "anonymousUser" },
        )

    @PatchMapping("/{id}")
    fun updatePost(
        @PathVariable id: String,
        @Valid @RequestBody body: PostUpdateRequest,
        @AuthenticationPrincipal currentUserId: String,
    ): ApiResponse<PostResponse> =
        postService.updatePost(
            postId = id,
            accountId = currentUserId,
            title = body.title,
            body = body.body,
        )
}

