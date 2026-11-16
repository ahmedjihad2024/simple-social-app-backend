package com.example.socialapp.post

/**
 * Shared SQL fragments for post queries.
 *
 * Must be `const val` so they can be used inside @Query annotations.
 * Fragments contain no trailing commas, so put the comma in the query itself.
 */
object PostSql {

    /**
     * Put right after `FROM <table> p` (the post alias MUST be `p`).
     * An aggregate without GROUP BY always returns exactly one row,
     * and COUNT gives 0 (not null), so posts with no reactions get all zeros.
     */
    const val REACTION_JOIN = """
    LEFT JOIN LATERAL (
        SELECT COUNT(*) FILTER (WHERE l.type = 'LIKE')  AS like_count,
               COUNT(*) FILTER (WHERE l.type = 'LOVE')  AS love_count,
               COUNT(*) FILTER (WHERE l.type = 'HAHA')  AS haha_count,
               COUNT(*) FILTER (WHERE l.type = 'WOW')   AS wow_count,
               COUNT(*) FILTER (WHERE l.type = 'SAD')   AS sad_count,
               COUNT(*) FILTER (WHERE l.type = 'ANGRY') AS angry_count
        FROM posts_likes l
        WHERE l.post_id = p.id
    ) rc ON TRUE
    """

    /** Use in the SELECT list together with [REACTION_JOIN]. */
    const val REACTION_COLUMNS = """
        rc.like_count  AS "likeCount",
        rc.love_count  AS "loveCount",
        rc.haha_count  AS "hahaCount",
        rc.wow_count   AS "wowCount",
        rc.sad_count   AS "sadCount",
        rc.angry_count AS "angryCount"
    """

    /** For a brand-new post (createPost): no reactions yet, so no join needed. */
    const val EMPTY_REACTION_COLUMNS = """
        CAST(0 AS bigint) AS "likeCount",
        CAST(0 AS bigint) AS "loveCount",
        CAST(0 AS bigint) AS "hahaCount",
        CAST(0 AS bigint) AS "wowCount",
        CAST(0 AS bigint) AS "sadCount",
        CAST(0 AS bigint) AS "angryCount"
    """
}