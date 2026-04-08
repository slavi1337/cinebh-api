package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.entities.QGenre;
import com.cinebh.api.entities.QMovie;
import com.cinebh.api.entities.QMovieGenre;
import com.cinebh.api.entities.QMovieImage;
import com.cinebh.api.entities.enums.MovieStatus;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MovieQueryRepositoryImpl implements MovieQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QMovie movie = QMovie.movie;
    private final QMovieImage movieImage = QMovieImage.movieImage;
    private final QMovieGenre movieGenre = QMovieGenre.movieGenre;
    private final QGenre genre = QGenre.genre;

    @Override
    public List<HeroMovieResponse> findHeroMovies() {
        final List<UUID> movieIds = queryFactory
                .select(movie.id)
                .from(movie)
                .where(isCurrentlyShowing())
                .orderBy(randomOrder())
                .limit(3)
                .fetch();

        if (movieIds.isEmpty()) {
            return List.of();
        }

        return mapHeroMovies(movieIds);
    }

    @Override
    public PageResponse<MovieCardResponse> findCurrentlyShowing(int page, int size) {
        final long totalElements = Optional.ofNullable(
                queryFactory
                        .select(movie.count())
                        .from(movie)
                        .where(isCurrentlyShowing())
                        .fetchOne()
        ).orElse(0L);

        final List<UUID> movieIds = queryFactory
                .select(movie.id)
                .from(movie)
                .where(isCurrentlyShowing())
                .orderBy(movie.releaseDate.desc(), movie.title.asc())
                .offset((long) page * size)
                .limit(size)
                .fetch();

        final List<MovieCardResponse> items = movieIds.isEmpty()
                ? List.of()
                : mapMovieCards(movieIds);

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                calculateTotalPages(totalElements, size)
        );
    }

    @Override
    public PageResponse<MovieCardResponse> findUpcomingMovies(int page, int size) {
        final long totalElements = Optional.ofNullable(
                queryFactory
                        .select(movie.count())
                        .from(movie)
                        .where(isUpcoming())
                        .fetchOne()
        ).orElse(0L);

        final List<UUID> movieIds = queryFactory
                .select(movie.id)
                .from(movie)
                .where(isUpcoming())
                .orderBy(movie.releaseDate.asc(), movie.title.asc())
                .offset((long) page * size)
                .limit(size)
                .fetch();

        final List<MovieCardResponse> items = movieIds.isEmpty()
                ? List.of()
                : mapMovieCards(movieIds);

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                calculateTotalPages(totalElements, size)
        );
    }

    private List<HeroMovieResponse> mapHeroMovies(List<UUID> movieIds) {
        final List<Tuple> rows = queryFactory
                .select(
                        movie.id,
                        movie.title,
                        movie.synopsis,
                        movieImage.imageUrl,
                        genre.name
                )
                .from(movie)
                .leftJoin(movieImage).on(movieImage.movie.id.eq(movie.id).and(movieImage.isCover.isTrue()))
                .leftJoin(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                .leftJoin(genre).on(movieGenre.genre.id.eq(genre.id))
                .where(movie.id.in(movieIds))
                .fetch();

        final Map<UUID, HeroAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (UUID movieId : movieIds) {
            accumulatorMap.put(movieId, new HeroAccumulator());
        }

        for (Tuple row : rows) {
            final UUID movieId = row.get(movie.id);
            if (movieId == null) {
                continue;
            }

            final HeroAccumulator accumulator = accumulatorMap.get(movieId);
            if (accumulator == null) {
                continue;
            }

            accumulator.id = movieId;
            accumulator.title = row.get(movie.title);
            accumulator.description = row.get(movie.synopsis);
            accumulator.imageUrl = row.get(movieImage.imageUrl);

            final String genreName = row.get(genre.name);
            if (genreName != null && !genreName.isBlank()) {
                accumulator.genres.add(genreName);
            }
        }

        final List<HeroMovieResponse> result = new ArrayList<>();

        for (UUID movieId : movieIds) {
            final HeroAccumulator accumulator = accumulatorMap.get(movieId);
            if (accumulator == null) {
                continue;
            }

            result.add(new HeroMovieResponse(
                    accumulator.id,
                    accumulator.title,
                    accumulator.description == null ? "" : accumulator.description,
                    new ArrayList<>(accumulator.genres),
                    accumulator.imageUrl
            ));
        }

        return result;
    }

    private List<MovieCardResponse> mapMovieCards(List<UUID> movieIds) {
        final List<Tuple> rows = queryFactory
                .select(
                        movie.id,
                        movie.title,
                        movie.durationMinutes,
                        movieImage.imageUrl,
                        genre.name
                )
                .from(movie)
                .leftJoin(movieImage).on(movieImage.movie.id.eq(movie.id).and(movieImage.isCover.isTrue()))
                .leftJoin(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                .leftJoin(genre).on(movieGenre.genre.id.eq(genre.id))
                .where(movie.id.in(movieIds))
                .fetch();

        final Map<UUID, MovieCardAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (UUID movieId : movieIds) {
            accumulatorMap.put(movieId, new MovieCardAccumulator());
        }

        for (Tuple row : rows) {
            final UUID movieId = row.get(movie.id);
            if (movieId == null) {
                continue;
            }

            final MovieCardAccumulator accumulator = accumulatorMap.get(movieId);
            if (accumulator == null) {
                continue;
            }

            accumulator.id = movieId;
            accumulator.title = row.get(movie.title);
            accumulator.durationMinutes = row.get(movie.durationMinutes);
            accumulator.coverImageUrl = row.get(movieImage.imageUrl);

            final String genreName = row.get(genre.name);
            if (genreName != null && !genreName.isBlank()) {
                accumulator.genres.add(genreName);
            }
        }

        final List<MovieCardResponse> result = new ArrayList<>();

        for (UUID movieId : movieIds) {
            final MovieCardAccumulator accumulator = accumulatorMap.get(movieId);
            if (accumulator == null) {
                continue;
            }

            result.add(new MovieCardResponse(
                    accumulator.id,
                    accumulator.title,
                    accumulator.durationMinutes,
                    accumulator.genres.isEmpty() ? null : String.join(", ", accumulator.genres),
                    accumulator.coverImageUrl
            ));
        }

        return result;
    }

    private BooleanExpression isCurrentlyShowing() {
        return movie.status.eq(MovieStatus.PUBLISHED)
                .and(movie.releaseDate.loe(LocalDate.now()))
                .and(movie.endDate.isNull().or(movie.endDate.goe(LocalDate.now())));
    }

    private BooleanExpression isUpcoming() {
        return movie.status.eq(MovieStatus.PUBLISHED)
                .and(movie.releaseDate.gt(LocalDate.now()));
    }

    private OrderSpecifier<Double> randomOrder() {
        return Expressions.numberTemplate(Double.class, "function('random')").asc();
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (size <= 0) {
            return 0;
        }

        return (int) Math.ceil((double) totalElements / size);
    }

    private static class HeroAccumulator {
        private final Set<String> genres = new LinkedHashSet<>();
        private UUID id;
        private String title;
        private String description;
        private String imageUrl;
    }

    private static class MovieCardAccumulator {
        private final Set<String> genres = new LinkedHashSet<>();
        private UUID id;
        private String title;
        private Integer durationMinutes;
        private String coverImageUrl;
    }
}
