CREATE TABLE articles (
  id          SERIAL PRIMARY KEY,
  slug        VARCHAR(255) NOT NULL UNIQUE,
  title       VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  body        TEXT NOT NULL,
  author_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);