CREATE TABLE IF NOT EXISTS animes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS sharacters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    anime_id BIGINT NOT NULL,
    CONSTRAINT fk_sharacters_anime FOREIGN KEY (anime_id) REFERENCES animes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sharacters_anime_id ON sharacters(anime_id);
