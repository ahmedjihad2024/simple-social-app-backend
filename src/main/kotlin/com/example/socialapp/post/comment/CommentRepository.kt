package com.example.socialapp.post.comment

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface CommentRepository : JpaRepository<PostComment, UUID> {

    @Query(
        value = """
            select c.id          as "id",
                   c.post_id     as "postId",
                   c.account_id  as "accountId",
                   c.parent_id   as "parentId",
                   c.comment     as "comment",
                   c.created_at  as "createdAt",
                   (p.first_name || ' ' || p.last_name) as "name",
                   p.avatar_filename as "avatarFilename",
                   exists (select 1 from posts_comments r where r.parent_id = c.id) as "hasReplies"
            from posts_comments c join user_profiles p on p.account_id = c.account_id
            where c.post_id = :postId
              and (c.parent_id = cast(:parentId as uuid)
                   or (cast(:parentId as text) is null and c.parent_id is null))
            order by c.created_at desc, c.id desc
        """,
        countQuery = """
            select count(*)
            from posts_comments c
            where c.post_id = :postId
              and (c.parent_id = cast(:parentId as uuid)
                   or (cast(:parentId as text) is null and c.parent_id is null))
        """,
        nativeQuery = true
    )
    fun findComments(
        @Param("postId") postId: UUID,
        @Param("parentId") parentId: String?,
        pageable: Pageable,
    ): Page<CommentProjection>

    @Query(
        """
        WITH new_comment AS (
            INSERT INTO posts_comments (account_id, post_id, parent_id, comment)
            VALUES (:accountId, :postId, :parentId, :comment)
            RETURNING id, post_id, account_id, parent_id, comment, created_at
        )
        SELECT id         AS "id",
               post_id    AS "postId",
               account_id AS "accountId",
               parent_id  AS "parentId",
               comment    AS "comment",
               created_at AS "createdAt",
               false      AS "hasReplies",
               (p.first_name || ' ' || p.last_name) AS "name",
               p.avatar_filename AS "avatarFilename"
        FROM new_comment JOIN user_profiles p ON p.account_id = :accountId
        """,
        nativeQuery = true
    )
    fun createComment(
        @Param("accountId") accountId: UUID,
        @Param("postId") postId: UUID,
        @Param("parentId") parentId: UUID?,
        @Param("comment") comment: String,
    ): CommentProjection

    @Query(
        """
        WITH updated AS (
            UPDATE posts_comments
            SET comment = :comment
            WHERE id = :commentId
              AND post_id = :postId
              AND account_id = :accountId
            RETURNING id, post_id, account_id, parent_id, comment, created_at
        )
        SELECT u.id         AS "id",
               u.post_id    AS "postId",
               u.account_id AS "accountId",
               u.parent_id  AS "parentId",
               u.comment    AS "comment",
               u.created_at AS "createdAt",
               EXISTS (SELECT 1 FROM posts_comments r WHERE r.parent_id = u.id) AS "hasReplies",
               (p.first_name || ' ' || p.last_name) AS "name",
               p.avatar_filename AS "avatarFilename"
        FROM updated u
        LEFT JOIN user_profiles p ON p.account_id = :accountId
        """,
        nativeQuery = true
    )
    fun updateComment(
        @Param("commentId") commentId: UUID,
        @Param("postId") postId: UUID,
        @Param("accountId") accountId: UUID,
        @Param("comment") comment: String,
    ): CommentProjection?

    @Query("SELECT c.post.id FROM PostComment c WHERE c.id = :id")
    fun findPostIdById(@Param("id") id: UUID): UUID?
}

interface CommentProjection {
    val id: UUID
    val postId: UUID
    val accountId: UUID
    val parentId: UUID?
    val comment: String
    val createdAt: Instant
    val hasReplies: Boolean
    val name: String?
    val avatarFilename: String?
}