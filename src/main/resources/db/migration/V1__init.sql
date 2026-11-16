-- Posts
CREATE TABLE IF NOT EXISTS posts
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID         NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    body        TEXT         NOT NULL,
    updated_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);


CREATE INDEX IF NOT EXISTS idx_posts_account_id ON posts (account_id);


-- Like of Posts
CREATE TABLE IF NOT EXISTS posts_likes
(
    account_id UUID        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    post_id    UUID        NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    type       VARCHAR(15) NOT NULL CHECK (type IN ('LIKE', 'LOVE', 'HAHA', 'WOW', 'SAD', 'ANGRY')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_posts_likes_post_id ON posts_likes (post_id);

-- Comments of Posts
CREATE TABLE IF NOT EXISTS posts_comments
(
    id         UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    account_id UUID          NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    post_id    UUID          NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    comment    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_posts_comments_post_id_created_at ON posts_comments (post_id, created_at DESC);

-- Images of Posts
CREATE TABLE IF NOT EXISTS posts_images
(
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id  UUID         NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    url      VARCHAR(255) NOT NULL,
    position INT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_posts_images_post_id ON posts_images (post_id);
