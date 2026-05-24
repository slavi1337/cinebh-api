ALTER TABLE movies
    ADD COLUMN imdb_rating            NUMERIC(3, 1),
    ADD COLUMN rotten_tomatoes_rating INTEGER;
ALTER TABLE movies
    ADD CONSTRAINT chk_movies_imdb_rating_range
        CHECK (imdb_rating IS NULL OR (imdb_rating >= 0 AND imdb_rating <= 10));
ALTER TABLE movies
    ADD CONSTRAINT chk_movies_rotten_tomatoes_rating_range
        CHECK (rotten_tomatoes_rating IS NULL OR (rotten_tomatoes_rating >= 0 AND rotten_tomatoes_rating <= 100));
