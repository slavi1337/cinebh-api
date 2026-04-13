package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingFilterOptionResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMovieResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesFiltersResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesSearchRequest;
import com.cinebh.api.entities.QCity;
import com.cinebh.api.entities.QGenre;
import com.cinebh.api.entities.QHall;
import com.cinebh.api.entities.QMovie;
import com.cinebh.api.entities.QMovieGenre;
import com.cinebh.api.entities.QMovieImage;
import com.cinebh.api.entities.QProjection;
import com.cinebh.api.entities.QVenue;
import com.cinebh.api.entities.enums.MovieStatus;
import com.cinebh.api.utils.PaginationUtils;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
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
public class UpcomingMoviesQueryRepositoryImpl implements UpcomingMoviesQueryRepository {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("Europe/Sarajevo");

    private final JPAQueryFactory queryFactory;

    private final QProjection projection = QProjection.projection;
    private final QMovie movie = QMovie.movie;
    private final QMovieImage movieImage = QMovieImage.movieImage;
    private final QMovieGenre movieGenre = QMovieGenre.movieGenre;
    private final QGenre genre = QGenre.genre;
    private final QHall hall = QHall.hall;
    private final QVenue venue = QVenue.venue;
    private final QCity city = QCity.city;

    @Override
    public PageResponse<UpcomingMovieResponse> findUpcomingMovies(
            final UpcomingMoviesSearchRequest searchRequest,
            final int page,
            final int size
    ) {
        final BooleanExpression predicate = buildPredicate(searchRequest);

        final long totalElements = Optional.ofNullable(
                applyProjectionVenueCityGraph(queryFactory.select(movie.id.countDistinct()))
                        .leftJoin(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                        .where(predicate)
                        .fetchOne()
        ).orElse(0L);

        final List<UUID> movieIds = applyProjectionVenueCityGraph(queryFactory.select(movie.id))
                .leftJoin(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                .where(predicate)
                .groupBy(movie.id, movie.title, movie.releaseDate)
                .orderBy(movie.releaseDate.asc(), movie.title.asc())
                .offset((long) page * size)
                .limit(size)
                .fetch();

        if (movieIds.isEmpty()) {
            return new PageResponse<>(
                    List.of(),
                    page,
                    size,
                    totalElements,
                    PaginationUtils.calculateTotalPages(totalElements, size)
            );
        }

        final List<UpcomingMovieResponse> items = mapUpcomingMovies(movieIds);

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                PaginationUtils.calculateTotalPages(totalElements, size)
        );
    }

    @Override
    public UpcomingMoviesFiltersResponse findFilters() {
        final BooleanExpression predicate = baseUpcomingPredicate();

        final List<UpcomingFilterOptionResponse> cities = applyProjectionVenueCityGraph(
                queryFactory.select(Projections.constructor(
                        UpcomingFilterOptionResponse.class,
                        city.id,
                        city.name
                ))
        )
                .where(predicate)
                .groupBy(city.id, city.name)
                .orderBy(city.name.asc())
                .fetch();

        final List<UpcomingFilterOptionResponse> venues = findUpcomingVenueOptions(predicate);

        final List<UpcomingFilterOptionResponse> genres = applyProjectionVenueCityGraph(
                queryFactory.select(Projections.constructor(
                        UpcomingFilterOptionResponse.class,
                        genre.id,
                        genre.name
                ))
        )
                .join(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                .join(genre).on(movieGenre.genre.id.eq(genre.id))
                .where(predicate)
                .groupBy(genre.id, genre.name)
                .orderBy(genre.name.asc())
                .fetch();

        return new UpcomingMoviesFiltersResponse(cities, venues, genres);
    }

    @Override
    public List<UpcomingFilterOptionResponse> findVenuesByCityIds(final List<UUID> cityIds) {
        final BooleanExpression basePredicate = baseUpcomingPredicate();
        final BooleanExpression predicate = cityIds == null || cityIds.isEmpty()
                ? basePredicate
                : basePredicate.and(city.id.in(cityIds));

        return findUpcomingVenueOptions(predicate);
    }

    private List<UpcomingFilterOptionResponse> findUpcomingVenueOptions(
            final BooleanExpression predicate
    ) {
        return applyProjectionVenueCityGraph(
                queryFactory.select(Projections.constructor(
                        UpcomingFilterOptionResponse.class,
                        venue.id,
                        venue.name
                ))
        )
                .where(predicate)
                .groupBy(venue.id, venue.name)
                .orderBy(venue.name.asc())
                .fetch();
    }

    private List<UpcomingMovieResponse> mapUpcomingMovies(final List<UUID> movieIds) {
        final List<Tuple> rows = queryFactory
                .select(
                        movie.id,
                        movie.title,
                        movie.durationMinutes,
                        movie.releaseDate,
                        movieImage.imageUrl,
                        genre.name
                )
                .from(movie)
                .leftJoin(movieImage).on(movieImage.movie.id.eq(movie.id).and(movieImage.isCover.isTrue()))
                .leftJoin(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                .leftJoin(genre).on(movieGenre.genre.id.eq(genre.id))
                .where(movie.id.in(movieIds))
                .fetch();

        final Map<UUID, UpcomingMovieAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (final Tuple row : rows) {
            final UUID movieId = row.get(movie.id);
            if (movieId == null) {
                continue;
            }

            final UpcomingMovieAccumulator accumulator =
                    accumulatorMap.computeIfAbsent(movieId, ignored -> new UpcomingMovieAccumulator());

            accumulator.movieId = movieId;
            accumulator.title = row.get(movie.title);
            accumulator.durationMinutes = row.get(movie.durationMinutes);
            accumulator.openingDate = row.get(movie.releaseDate);
            accumulator.posterImageUrl = row.get(movieImage.imageUrl);

            final String genreName = row.get(genre.name);
            if (genreName != null && !genreName.isBlank()) {
                accumulator.genres.add(genreName);
            }
        }

        final List<UpcomingMovieResponse> result = new ArrayList<>();

        for (final UUID movieId : movieIds) {
            final UpcomingMovieAccumulator accumulator = accumulatorMap.get(movieId);
            if (accumulator == null) {
                continue;
            }

            result.add(new UpcomingMovieResponse(
                    accumulator.movieId,
                    accumulator.title,
                    accumulator.posterImageUrl,
                    accumulator.durationMinutes,
                    new ArrayList<>(accumulator.genres),
                    accumulator.openingDate
            ));
        }

        return result;
    }

    private BooleanExpression buildPredicate(final UpcomingMoviesSearchRequest searchRequest) {
        BooleanExpression predicate = baseUpcomingPredicate();

        if (searchRequest.query() != null && !searchRequest.query().isBlank()) {
            predicate = predicate.and(movie.title.containsIgnoreCase(searchRequest.query().trim()));
        }

        if (searchRequest.cityIds() != null && !searchRequest.cityIds().isEmpty()) {
            predicate = predicate.and(city.id.in(searchRequest.cityIds()));
        }

        if (searchRequest.venueIds() != null && !searchRequest.venueIds().isEmpty()) {
            predicate = predicate.and(venue.id.in(searchRequest.venueIds()));
        }

        if (searchRequest.genreIds() != null && !searchRequest.genreIds().isEmpty()) {
            predicate = predicate.and(movieGenre.genre.id.in(searchRequest.genreIds()));
        }

        if (searchRequest.startDate() != null) {
            predicate = predicate.and(movie.releaseDate.goe(searchRequest.startDate()));
        }

        if (searchRequest.endDate() != null) {
            predicate = predicate.and(movie.releaseDate.loe(searchRequest.endDate()));
        }

        return predicate;
    }

    private BooleanExpression baseUpcomingPredicate() {
        final LocalDate today = LocalDate.now(CINEMA_ZONE);

        return movie.status.eq(MovieStatus.PUBLISHED)
                .and(movie.releaseDate.gt(today))
                .and(projection.startTime.goe(today.atStartOfDay(CINEMA_ZONE).toOffsetDateTime()));
    }

    private <T> JPAQuery<T> applyProjectionVenueCityGraph(final JPAQuery<T> query) {
        return query.from(projection)
                .join(projection.movie, movie)
                .join(projection.hall, hall)
                .join(hall.venue, venue)
                .join(venue.city, city);
    }

    private static class UpcomingMovieAccumulator {
        private final Set<String> genres = new LinkedHashSet<>();
        private UUID movieId;
        private String title;
        private String posterImageUrl;
        private Integer durationMinutes;
        private LocalDate openingDate;
    }
}
