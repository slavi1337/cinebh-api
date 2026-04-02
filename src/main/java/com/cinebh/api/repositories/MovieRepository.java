package com.cinebh.api.repositories;

import com.cinebh.api.entities.Movie;
import com.cinebh.api.repositories.projections.HeroMovieProjection;
import com.cinebh.api.repositories.projections.MovieCardProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    @Query(value = """
            SELECT
                m.id AS id,
                m.title AS title,
                COALESCE(m.synopsis, '') AS description,
                COALESCE(STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name), '') AS genresCsv,
                mi.image_url AS imageUrl
            FROM movies m
            LEFT JOIN movie_genres mg ON mg.movie_id = m.id
            LEFT JOIN genres g ON g.id = mg.genre_id
            LEFT JOIN movie_images mi ON mi.movie_id = m.id AND mi.is_cover = true
            WHERE m.status = 'PUBLISHED'
              AND m.release_date <= CURRENT_DATE
              AND (m.end_date IS NULL OR m.end_date >= CURRENT_DATE)
            GROUP BY m.id, m.title, m.synopsis, mi.image_url
            ORDER BY random()
            LIMIT 3
            """, nativeQuery = true)
    List<HeroMovieProjection> findHeroMovies();

    @Query(value = """
            SELECT
                m.id AS id,
                m.title AS title,
                m.duration_minutes AS durationMinutes,
                COALESCE(STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name), '') AS genresCsv,
                mi.image_url AS coverImageUrl
            FROM movies m
            LEFT JOIN movie_genres mg ON mg.movie_id = m.id
            LEFT JOIN genres g ON g.id = mg.genre_id
            LEFT JOIN movie_images mi ON mi.movie_id = m.id AND mi.is_cover = true
            WHERE m.status = 'PUBLISHED'
              AND m.release_date <= CURRENT_DATE
              AND (m.end_date IS NULL OR m.end_date >= CURRENT_DATE)
            GROUP BY m.id, m.title, m.duration_minutes, mi.image_url, m.release_date
            ORDER BY m.release_date DESC, m.title ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM movies m
            WHERE m.status = 'PUBLISHED'
              AND m.release_date <= CURRENT_DATE
              AND (m.end_date IS NULL OR m.end_date >= CURRENT_DATE)
            """,
            nativeQuery = true)
    Page<MovieCardProjection> findCurrentlyShowing(Pageable pageable);

    @Query(value = """
            SELECT
                m.id AS id,
                m.title AS title,
                m.duration_minutes AS durationMinutes,
                COALESCE(STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name), '') AS genresCsv,
                mi.image_url AS coverImageUrl
            FROM movies m
            LEFT JOIN movie_genres mg ON mg.movie_id = m.id
            LEFT JOIN genres g ON g.id = mg.genre_id
            LEFT JOIN movie_images mi ON mi.movie_id = m.id AND mi.is_cover = true
            WHERE m.status = 'PUBLISHED'
              AND m.release_date > CURRENT_DATE
            GROUP BY m.id, m.title, m.duration_minutes, mi.image_url, m.release_date
            ORDER BY m.release_date ASC, m.title ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM movies m
            WHERE m.status = 'PUBLISHED'
              AND m.release_date > CURRENT_DATE
            """,
            nativeQuery = true)
    Page<MovieCardProjection> findUpcomingMovies(Pageable pageable);
}
