package com.example.socialapp.post.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

@Schema(description = "Request body for creating or updating a post")
data class PostCreateRequest(
    @field:Schema(description = "Post title", example = "Exciting news about our project")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 3, max = 255, message = "{validation.post.title.size}")
    val title: String,

    @field:Schema(description = "Post body content", example = "This is the content of the post...")
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:Size(min = 1, max = 10000, message = "{validation.post.body.size}")
    val body: String,

    @field:Schema(description = "Optional list of image files to attach to the post (up to 10 images)")
    @field:Size(max = 10, message = "{validation.post.images.max}")
    val images: List<MultipartFile>? = null,
)

