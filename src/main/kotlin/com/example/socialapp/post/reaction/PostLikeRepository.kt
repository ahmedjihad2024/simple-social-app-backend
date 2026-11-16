package com.example.socialapp.post.reaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PostLikeRepository : JpaRepository<PostLike, PostLikeId> {

    @Query(
        """
        WITH upsert AS (
            INSERT INTO posts_likes (account_id, post_id, type)
            VALUES (:accountId, :postId, :type)
            ON CONFLICT (account_id, post_id)
            DO UPDATE SET type = EXCLUDED.type
            RETURNING type
        )
        SELECT
            (SELECT count(*)
             FROM posts_likes l
             WHERE l.post_id = :postId
               AND l.account_id <> :accountId) + 1 AS "likesCount",
            TRUE                                  AS "likedByMe",
            u.type                                AS "myReaction"
        FROM upsert u
        """,
        nativeQuery = true
    )
    fun react(
        @Param("accountId") accountId: UUID,
        @Param("postId") postId: UUID,
        @Param("type") type: String,
    ): ReactProjection

    @Query(
        """
        WITH counts AS (
            SELECT
                COUNT(*) FILTER (WHERE l.type = 'LIKE')  AS like_count,
                COUNT(*) FILTER (WHERE l.type = 'LOVE')  AS love_count,
                COUNT(*) FILTER (WHERE l.type = 'HAHA')  AS haha_count,
                COUNT(*) FILTER (WHERE l.type = 'WOW')   AS wow_count,
                COUNT(*) FILTER (WHERE l.type = 'SAD')   AS sad_count,
                COUNT(*) FILTER (WHERE l.type = 'ANGRY') AS angry_count
            FROM posts_likes l
            WHERE l.post_id = :postId
        )
        SELECT
            c.like_count                AS "likeCount",
            c.love_count                AS "loveCount",
            c.haha_count                AS "hahaCount",
            c.wow_count                 AS "wowCount",
            c.sad_count                 AS "sadCount",
            c.angry_count               AS "angryCount",
            mine.account_id IS NOT NULL AS "likedByMe",
            mine.type                   AS "myReaction"
        FROM counts c
        LEFT JOIN posts_likes mine
               ON mine.post_id = :postId
              AND mine.account_id = cast(:viewerId as uuid)
        """,
        nativeQuery = true
    )
    fun getReactions(
        @Param("postId") postId: UUID,
        @Param("viewerId") viewerId: UUID?,
    ): ReactBreakdownProjection
}

interface ReactProjection {
    val likesCount: Long
    val likedByMe: Boolean
    val myReaction: Reaction?
}

interface ReactBreakdownProjection {
    val likeCount: Long
    val loveCount: Long
    val hahaCount: Long
    val wowCount: Long
    val sadCount: Long
    val angryCount: Long
    val likedByMe: Boolean
    val myReaction: Reaction?
}

