ALTER TABLE posts_comments
  ADD COLUMN parent_id UUID REFERENCES posts_comments(id) ON DELETE CASCADE;

CREATE INDEX idx_posts_comments_parent_id_created_at
  ON posts_comments (parent_id, created_at);

CREATE INDEX idx_posts_comments_post_top_level
  ON posts_comments (post_id, created_at DESC) WHERE parent_id IS NULL;