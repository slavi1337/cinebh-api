package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.FilterResponse;
import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingFiltersResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingMovieResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingSearchRequest;
import com.cinebh.api.dto.currentlyshowing.ProjectionTimeResponse;
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
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
public class CurrentlyShowingQueryRepositoryImpl implements CurrentlyShowingQueryRepository {

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
    public PageResponse<CurrentlyShowingMovieResponse> findCurrentlyShowing(
            final CurrentlyShowingSearchRequest searchRequest,
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
                .groupBy(movie.id, movie.title)
                .orderBy(movie.title.asc())
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

        final List<CurrentlyShowingMovieResponse> items = mapCurrentlyShowingMovies(movieIds, searchRequest);

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                PaginationUtils.calculateTotalPages(totalElements, size)
        );
    }

    @Override
    public CurrentlyShowingFiltersResponse findFilters() {
        final List<FilterResponse> cities = queryFactory
                .select(Projections.constructor(
                        FilterResponse.class,
                        city.id,
                        city.name
                ))
                .from(city)
                .orderBy(city.name.asc())
                .fetch();

        final List<FilterResponse> venues = findVenueFilterOptions(null);

        final List<FilterResponse> genres = queryFactory
                .select(Projections.constructor(
                        FilterResponse.class,
                        genre.id,
                        genre.name
                ))
                .from(genre)
                .orderBy(genre.name.asc())
                .fetch();

        return new CurrentlyShowingFiltersResponse(cities, venues, genres);
    }
    
    @Override
    public List<FilterResponse> findVenuesByCityIds(final List<UUID> cityIds) {
        return findVenueFilterOptions(cityIds);
    }

    private List<FilterResponse> findVenueFilterOptions(final List<UUID> cityIds) {
        final JPAQuery<FilterResponse> query = queryFactory
                .select(Projections.constructor(
                        FilterResponse.class,
                        venue.id,
                        venueLabelExpression(),
                        city.id
                ))
                .from(venue)
                .join(venue.city, city);

        if (cityIds != null && !cityIds.isEmpty()) {
            query.where(city.id.in(cityIds));
        }

        return query.orderBy(venue.name.asc(), city.name.asc()).fetch();
    }

    private List<CurrentlyShowingMovieResponse> mapCurrentlyShowingMovies(
            final List<UUID> movieIds,
            final CurrentlyShowingSearchRequest searchRequest
    ) {
        final List<Tuple> rows = queryFactory
                .select(
                        movie.id,
                        movie.title,
                        movie.durationMinutes,
                        movie.pgRating,
                        movie.language,
                        movie.endDate,
                        movieImage.imageUrl,
                        genre.name
                )
                .from(movie)
                .leftJoin(movieImage).on(movieImage.movie.id.eq(movie.id).and(movieImage.isCover.isTrue()))
                .leftJoin(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                .leftJoin(genre).on(movieGenre.genre.id.eq(genre.id))
                .where(movie.id.in(movieIds))
                .fetch();

        final Map<UUID, MovieAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (final Tuple row : rows) {
            final UUID movieId = row.get(movie.id);
            if (movieId == null) {
                continue;
            }

            final MovieAccumulator accumulator =
                    accumulatorMap.computeIfAbsent(movieId, ignored -> new MovieAccumulator());

            accumulator.movieId = movieId;
            accumulator.title = row.get(movie.title);
            accumulator.durationMinutes = row.get(movie.durationMinutes);
            accumulator.pgRating = row.get(movie.pgRating);
            accumulator.language = row.get(movie.language);
            accumulator.endDate = row.get(movie.endDate);
            accumulator.posterImageUrl = row.get(movieImage.imageUrl);

            final String genreName = row.get(genre.name);
            if (genreName != null && !genreName.isBlank()) {
                accumulator.genres.add(genreName);
            }
        }

        final Map<UUID, List<ProjectionTimeResponse>> showtimesByMovieId =
                getShowtimesByMovieIds(movieIds, searchRequest);

        final List<CurrentlyShowingMovieResponse> result = new ArrayList<>();

        for (final UUID movieId : movieIds) {
            final MovieAccumulator accumulator = accumulatorMap.get(movieId);
            if (accumulator == null) {
                continue;
            }

            result.add(new CurrentlyShowingMovieResponse(
                    accumulator.movieId,
                    accumulator.title,
                    accumulator.posterImageUrl,
                    accumulator.pgRating,
                    accumulator.language,
                    accumulator.durationMinutes,
                    new ArrayList<>(accumulator.genres),
                    accumulator.endDate,
                    showtimesByMovieId.getOrDefault(movieId, List.of())
            ));
        }

        return result;
    }

    private Map<UUID, List<ProjectionTimeResponse>> getShowtimesByMovieIds(
            final List<UUID> movieIds,
            final CurrentlyShowingSearchRequest searchRequest
    ) {
        final BooleanExpression showtimePredicate = buildPredicate(searchRequest)
                .and(movie.id.in(movieIds));

        final List<Tuple> rows = applyProjectionVenueCityGraph(
                queryFactory.select(
                        movie.id,
                        projection.id,
                        projection.startTime,
                        venue.id,
                        venue.name,
                        city.id,
                        city.name
                )
        )
                .leftJoin(movieGenre).on(movieGenre.movie.id.eq(movie.id))
                .where(showtimePredicate)
                .orderBy(projection.startTime.asc())
                .fetch();

        final Map<UUID, List<ProjectionTimeResponse>> result = new LinkedHashMap<>();
        final Set<UUID> seenProjectionIds = new LinkedHashSet<>();

        for (final Tuple row : rows) {
            final UUID movieId = row.get(movie.id);
            final UUID projectionId = row.get(projection.id);

            if (movieId == null || projectionId == null || seenProjectionIds.contains(projectionId)) {
                continue;
            }

            final OffsetDateTime projectionStartTime = row.get(projection.startTime);
            if (projectionStartTime == null) {
                continue;
            }

            seenProjectionIds.add(projectionId);

            final ProjectionTimeResponse showtime = new ProjectionTimeResponse(
                    projectionId,
                    projectionStartTime.atZoneSameInstant(CINEMA_ZONE).toLocalTime(),
                    row.get(venue.id),
                    row.get(venue.name),
                    row.get(city.id),
                    row.get(city.name)
            );

            result.computeIfAbsent(movieId, ignored -> new ArrayList<>()).add(showtime);
        }

        return result;
    }

    private BooleanExpression buildPredicate(final CurrentlyShowingSearchRequest searchRequest) {
        BooleanExpression predicate = movie.status.eq(MovieStatus.PUBLISHED)
                .and(isSelectedDate(searchRequest.date()))
                .and(isWithinMovieWindow(searchRequest.date()));

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

        if (searchRequest.projectionTimes() != null && !searchRequest.projectionTimes().isEmpty()) {
            predicate = predicate.and(hasProjectionTimes(searchRequest.date(), searchRequest.projectionTimes()));
        }

        return predicate;
    }

    private BooleanExpression isWithinMovieWindow(final LocalDate date) {
        return movie.releaseDate.loe(date)
                .and(movie.endDate.isNull().or(movie.endDate.goe(date)));
    }

    private BooleanExpression isSelectedDate(final LocalDate date) {
        final OffsetDateTime startOfDay = date.atStartOfDay(CINEMA_ZONE).toOffsetDateTime();
        final OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(CINEMA_ZONE).toOffsetDateTime();

        return projection.startTime.goe(startOfDay)
                .and(projection.startTime.lt(endOfDay));
    }

    private BooleanExpression hasProjectionTimes(
            final LocalDate date,
            final List<LocalTime> projectionTimes
    ) {
        BooleanExpression predicate = null;

        for (final LocalTime projectionTime : projectionTimes) {
            final OffsetDateTime selectedDateTime =
                    date.atTime(projectionTime).atZone(CINEMA_ZONE).toOffsetDateTime();

            final BooleanExpression timePredicate = projection.startTime.eq(selectedDateTime);
            predicate = predicate == null ? timePredicate : predicate.or(timePredicate);
        }

        return predicate;
    }

    private <T> JPAQuery<T> applyProjectionVenueCityGraph(final JPAQuery<T> query) {
        return query.from(projection)
                .join(projection.movie, movie)
                .join(projection.hall, hall)
                .join(hall.venue, venue)
                .join(venue.city, city);
    }

    private StringExpression venueLabelExpression() {
        return Expressions.stringTemplate(
                "concat({0}, ' (', {1}, ')')",
                venue.name,
                city.name
        );
    }

    private static class MovieAccumulator {
        private final Set<String> genres = new LinkedHashSet<>();
        private UUID movieId;
        private String title;
        private String posterImageUrl;
        private String pgRating;
        private String language;
        private Integer durationMinutes;
        private LocalDate endDate;
    }
}
