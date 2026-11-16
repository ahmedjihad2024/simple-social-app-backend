package com.example.socialapp.post.comment.mapper

import com.example.socialapp.common.extentions.toMediaUrl
import com.example.socialapp.common.storage.FileCategory
import com.example.socialapp.post.comment.CommentProjection
import com.example.socialapp.post.comment.PostComment
import com.example.socialapp.post.comment.dto.CommentAccountResponse
import com.example.socialapp.post.comment.dto.CommentResponse

fun CommentProjection.toResponse(): CommentResponse = CommentResponse(
    id = id,
//    postId = postId,
    account = CommentAccountResponse(
        id = accountId,
        name = name?.trim()?.ifBlank { null },
        imageUrl = avatarFilename.toMediaUrl(FileCategory.AVATAR),
    ),
    comment = comment,
//    parentId = parentId,
    hasReplies = hasReplies,
    createdAt = createdAt,
)

fun PostComment.toResponse(
    name: String? = null,
    avatarFilename: String? = null,
    hasReplies: Boolean = false,
): CommentResponse = CommentResponse(
    id = id!!,
//    postId = post.id!!,
    account = CommentAccountResponse(
        id = account.id!!,
        name = name?.trim()?.ifBlank { null },
        imageUrl = avatarFilename.toMediaUrl(FileCategory.AVATAR),
    ),
    comment = comment,
//    parentId = parent?.id,
    hasReplies = hasReplies,
    createdAt = createdAt,
)
