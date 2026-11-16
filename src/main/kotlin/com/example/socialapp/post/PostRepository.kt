package com.example.socialapp.post

import com.example.socialapp.post.entites.Post
import com.example.socialapp.post.entites.PostImage
import com.example.socialapp.post.reaction.Reaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID
interface PostRepository : JpaRepository<Post, UUID> {

    @Query(
        """
    WITH new_post AS (
        INSERT INTO posts (account_id, title, body)
        VALUES (:accountId, :title, :body)
        RETURNING id, account_id, title, body, created_at, updated_at
    ),
    new_images AS (
        INSERT INTO posts_images (post_id, url, position)
        SELECT new_post.id, img.url, img.pos - 1
        FROM new_post,
             unnest(cast(:images AS text[])) WITH ORDINALITY AS img(url, pos)
    )
    SELECT np.id                   AS "id",
           np.account_id           AS "accountId",
           np.title                AS "title",
           np.body                 AS "body",
           np.created_at           AS "createdAt",
           np.updated_at           AS "updatedAt",
           cast(:images AS text[]) AS "images",
           CAST(0 AS bigint)       AS "commentsCount",
           ${PostSql.EMPTY_REACTION_COLUMNS},
           false                   AS "likedByMe",
           cast(null AS varchar)   AS "myReaction",
           TRIM(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) AS "name",
           u.avatar_filename       AS "avatarFilename"
    FROM new_post np
    LEFT JOIN user_profiles u ON u.account_id = :accountId
    """,
        nativeQuery = true
    )
    fun createPost(
        @Param("accountId") accountId: UUID,
        @Param("title") title: String,
        @Param("body") body: String,
        @Param("images") images: Array<String>,
    ): PostDetailsProjection


    @Query(
        """
    SELECT p.id         AS "id",
           p.account_id AS "accountId",
           p.title      AS "title",
           p.body       AS "body",
           p.created_at AS "createdAt",
           p.updated_at AS "updatedAt",
           COALESCE(
               (SELECT array_agg(i.url ORDER BY i.position)
                FROM posts_images i
                WHERE i.post_id = p.id),
               '{}'
           )            AS "images",
           (SELECT count(*) FROM posts_comments pc WHERE pc.post_id = p.id) AS "commentsCount",
           ${PostSql.REACTION_COLUMNS},
           mine.account_id IS NOT NULL AS "likedByMe",
           mine.type                   AS "myReaction",
           TRIM(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) AS "name",
           u.avatar_filename          AS "avatarFilename"
    FROM posts p
    LEFT JOIN user_profiles u ON u.account_id = p.account_id
    ${PostSql.REACTION_JOIN}
    LEFT JOIN posts_likes mine
           ON mine.post_id = p.id
          AND mine.account_id = cast(:viewerId as uuid)
    WHERE p.id = cast(:postId as uuid)
    """,
        nativeQuery = true
    )
    fun postDetails(
        @Param("postId") postId: UUID,
        @Param("viewerId") viewerId: UUID,
    ): PostDetailsProjection?


    @Query(
        value = """
    SELECT p.id         AS "id",
           p.account_id AS "accountId",
           p.title      AS "title",
           p.body       AS "body",
           p.created_at AS "createdAt",
           p.updated_at AS "updatedAt",
           COALESCE(
               (SELECT array_agg(i.url ORDER BY i.position)
                FROM posts_images i
                WHERE i.post_id = p.id),
               '{}'
           )            AS "images",
           (SELECT count(*) FROM posts_comments pc WHERE pc.post_id = p.id) AS "commentsCount",
           ${PostSql.REACTION_COLUMNS},
           mine.account_id IS NOT NULL AS "likedByMe",
           mine.type                   AS "myReaction",
           TRIM(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) AS "name",
           u.avatar_filename          AS "avatarFilename"
    FROM posts p
    LEFT JOIN user_profiles u ON u.account_id = p.account_id
    ${PostSql.REACTION_JOIN}
    LEFT JOIN posts_likes mine
           ON mine.post_id = p.id
          AND mine.account_id = cast(:viewerId as uuid)
    WHERE (cast(:accountId as uuid) IS NULL OR p.account_id = cast(:accountId as uuid))
    ORDER BY p.created_at DESC, p.id DESC
    """,
        countQuery = "SELECT count(*) FROM posts WHERE (cast(:accountId as uuid) IS NULL OR account_id = cast(:accountId as uuid))",
        nativeQuery = true
    )
    fun posts(
        @Param("viewerId") viewerId: UUID,
        @Param("accountId") accountId: UUID? = null,
        pageable: Pageable,
    ): Page<PostDetailsProjection>


    @Query(
        """
    WITH updated AS (
        UPDATE posts
        SET title      = COALESCE(cast(:title as varchar), title),
            body       = COALESCE(cast(:body as text), body),
            updated_at = now()
        WHERE id = :postId
          AND account_id = :accountId
        RETURNING id, account_id, title, body, created_at, updated_at
    )
    SELECT p.id         AS "id",
           p.account_id AS "accountId",
           p.title      AS "title",
           p.body       AS "body",
           p.created_at AS "createdAt",
           p.updated_at AS "updatedAt",
           COALESCE(
               (SELECT array_agg(i.url ORDER BY i.position)
                FROM posts_images i
                WHERE i.post_id = p.id),
               '{}'
           )            AS "images",
           (SELECT count(*) FROM posts_comments pc WHERE pc.post_id = p.id) AS "commentsCount",
           ${PostSql.REACTION_COLUMNS},
           mine.account_id IS NOT NULL AS "likedByMe",
           mine.type                   AS "myReaction",
           TRIM(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) AS "name",
           u.avatar_filename          AS "avatarFilename"
    FROM updated p
    LEFT JOIN user_profiles u ON u.account_id = :accountId
    ${PostSql.REACTION_JOIN}
    LEFT JOIN posts_likes mine
           ON mine.post_id = p.id
          AND mine.account_id = :accountId
    """,
        nativeQuery = true
    )
    fun updatePost(
        @Param("postId") postId: UUID,
        @Param("accountId") accountId: UUID,
        @Param("title") title: String?,
        @Param("body") body: String?,
    ): PostDetailsProjection?
}

interface PostDetailsProjection {
    val id: UUID
    val accountId: UUID
    val title: String
    val body: String
    val images: Array<String>
    val commentsCount: Long
    val likeCount: Long
    val loveCount: Long
    val hahaCount: Long
    val wowCount: Long
    val sadCount: Long
    val angryCount: Long
    val likedByMe: Boolean
    val myReaction: Reaction?
    val createdAt: Instant
    val updatedAt: Instant?
    val name: String?
    val avatarFilename: String?
}