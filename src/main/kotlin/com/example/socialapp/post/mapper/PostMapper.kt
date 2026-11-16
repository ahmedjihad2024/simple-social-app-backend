package com.example.socialapp.post.mapper

import com.example.socialapp.common.extentions.toMediaUrl
import com.example.socialapp.common.storage.FileCategory
import com.example.socialapp.post.PostDetailsProjection
import com.example.socialapp.post.dto.PostAccountResponse
import com.example.socialapp.post.dto.PostResponse
import com.example.socialapp.post.dto.ReactionsResponse
import com.example.socialapp.post.entites.Post
import com.example.socialapp.post.reaction.PostLikeRepository
import com.example.socialapp.post.reaction.ReactBreakdownProjection
import com.example.socialapp.post.reaction.ReactProjection
import com.example.socialapp.post.reaction.Reaction
import com.example.socialapp.post.reaction.dto.ReactResponse

fun PostDetailsProjection.toResponse(): PostResponse {
    val total = likeCount + loveCount + hahaCount + wowCount + sadCount + angryCount
    return PostResponse(
        id = id,
        account = PostAccountResponse(
            id = accountId,
            name = name?.trim()?.ifBlank { null },
            imageUrl = avatarFilename.toMediaUrl(FileCategory.AVATAR),
        ),
        title = title,
        body = body,
        images = images.mapNotNull { it.toMediaUrl(FileCategory.IMAGES) },
        commentsCount = commentsCount,
        reactions = ReactionsResponse(
            total = total,
            like = likeCount,
            love = loveCount,
            haha = hahaCount,
            wow = wowCount,
            sad = sadCount,
            angry = angryCount,
        ),
        likedByMe = likedByMe,
        myReaction = myReaction,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun Post.toResponse(
    name: String? = null,
    avatarFilename: String? = null,
    commentsCount: Long = 0,
    reactions: ReactionsResponse = ReactionsResponse(),
    likedByMe: Boolean = false,
    myReaction: Reaction? = null,
): PostResponse =
    PostResponse(
        id = id!!,
        account = PostAccountResponse(
            id = account.id!!,
            name = name?.trim()?.ifBlank { null },
            imageUrl = avatarFilename.toMediaUrl(FileCategory.AVATAR),
        ),
        title = title,
        body = body,
        images = images.sortedBy { it.position }.mapNotNull { it.url.toMediaUrl(FileCategory.IMAGES) },
        commentsCount = commentsCount,
        reactions = reactions,
        likedByMe = likedByMe,
        myReaction = myReaction,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ReactBreakdownProjection.toResponse(): ReactResponse {
    val total = likeCount + loveCount + hahaCount + wowCount + sadCount + angryCount
    return ReactResponse(
        reactions = ReactionsResponse(
            total = total,
            like = likeCount,
            love = loveCount,
            haha = hahaCount,
            wow = wowCount,
            sad = sadCount,
            angry = angryCount,
        ),
        likedByMe = likedByMe,
        myReaction = myReaction,
    )
}




