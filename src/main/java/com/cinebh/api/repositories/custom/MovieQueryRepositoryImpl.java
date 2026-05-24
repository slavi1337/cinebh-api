package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.dto.movie.MovieCastMemberResponse;
import com.cinebh.api.dto.movie.MovieDetailsFilterOptionResponse;
import com.cinebh.api.dto.movie.MovieDetailsResponse;
import com.cinebh.api.dto.movie.MovieProjectionResponse;
import com.cinebh.api.dto.movie.MovieProjectionSearchRequest;
import com.cinebh.api.entities.QCity;
import com.cinebh.api.entities.QGenre;
import com.cinebh.api.entities.QHall;
import com.cinebh.api.entities.QMovie;
import com.cinebh.api.entities.QMovieCast;
import com.cinebh.api.entities.QMovieDirector;
import com.cinebh.api.entities.QMovieGenre;
import com.cinebh.api.entities.QMovieImage;
import com.cinebh.api.entities.QMovieWriter;
import com.cinebh.api.entities.QPerson;
import com.cinebh.api.entities.QProjection;
import com.cinebh.api.entities.QVenue;
import com.cinebh.api.entities.enums.MovieStatus;
import com.cinebh.api.utils.PaginationUtils;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
public class MovieQueryRepositoryImpl implements MovieQueryRepository {

    private static final int SEE_ALSO_LIMIT = 10;
    private static final int PREVIEW_IMAGES_LIMIT = 4;
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Europe/Sarajevo");

    private final JPAQueryFactory queryFactory;

    private final QMovie movie = QMovie.movie;
    private final QMovieImage movieImage = QMovieImage.movieImage;
    private final QMovieGenre movieGenre = QMovieGenre.movieGenre;
    private final QGenre genre = QGenre.genre;
    private final QProjection projection = QProjection.projection;
    private final QHall hall = QHall.hall;
    private final QVenue venue = QVenue.venue;
    private final QCity city = QCity.city;
    private final QPerson person = QPerson.person;
    private final QMovieCast movieCast = QMovieCast.movieCast;
    private final QMovieDirector movieDirector = QMovieDirector.movieDirector;
    private final QMovieWriter movieWriter = QMovieWriter.movieWriter;

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
    public PageResponse<MovieCardResponse> findCurrentlyShowing(final int page, final int size) {
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
                PaginationUtils.calculateTotalPages(totalElements, size)
        );
    }

    @Override
    public PageResponse<MovieCardResponse> findUpcomingMovies(final int page, final int size) {
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
                PaginationUtils.calculateTotalPages(totalElements, size)
        );
    }

    @Override
    public Optional<MovieDetailsResponse> findMovieDetailsById(final UUID movieId) {
        final Tuple movieRow = queryFactory
                .select(
                        movie.id,
                        movie.title,
                        movie.synopsis,
                        movie.pgRating,
                        movie.language,
                        movie.durationMinutes,
                        movie.imdbRating,
                        movie.rottenTomatoesRating,
                        movie.releaseDate,
                        movie.endDate,
                        movie.trailerUrl
                )
                .from(movie)
                .where(movie.id.eq(movieId).and(movie.status.eq(MovieStatus.PUBLISHED)))
                .fetchOne();

        if (movieRow == null) {
            return Optional.empty();
        }

        final LocalDate releaseDate = movieRow.get(movie.releaseDate);
        final LocalDate endDate = movieRow.get(movie.endDate);

        final List<MovieCardResponse> seeAlso = findSeeAlsoMovies(movieId, releaseDate, endDate);

        return Optional.of(new MovieDetailsResponse(
                movieRow.get(movie.id),
                movieRow.get(movie.title),
                movieRow.get(movie.synopsis),
                movieRow.get(movie.pgRating),
                movieRow.get(movie.language),
                movieRow.get(movie.durationMinutes),
                movieRow.get(movie.imdbRating),
                movieRow.get(movie.rottenTomatoesRating),
                releaseDate,
                endDate,
                movieRow.get(movie.trailerUrl),
                findCoverImageUrl(movieId),
                findPreviewImageUrls(movieId),
                findMovieGenres(movieId),
                findMovieCast(movieId),
                findMovieDirectors(movieId),
                findMovieWriters(movieId),
                findMovieCities(movieId),
                findMovieVenues(movieId),
                findMovieProjectionDates(movieId),
                seeAlso
        ));
    }

    @Override
    public List<MovieProjectionResponse> findMovieProjections(
            final UUID movieId,
            final MovieProjectionSearchRequest searchRequest
    ) {
        BooleanExpression predicate = movie.id.eq(movieId)
                .and(movie.status.eq(MovieStatus.PUBLISHED))
                .and(isSelectedDate(searchRequest.date()));

        if (searchRequest.cityIds() != null && !searchRequest.cityIds().isEmpty()) {
            predicate = predicate.and(city.id.in(searchRequest.cityIds()));
        }

        if (searchRequest.venueIds() != null && !searchRequest.venueIds().isEmpty()) {
            predicate = predicate.and(venue.id.in(searchRequest.venueIds()));
        }

        final List<Tuple> rows = applyProjectionVenueCityGraph(
                queryFactory.select(
                        projection.id,
                        projection.startTime,
                        venue.id,
                        venue.name,
                        city.id,
                        city.name,
                        hall.id,
                        hall.name
                )
        )
                .where(predicate)
                .orderBy(projection.startTime.asc(), venue.name.asc(), hall.name.asc())
                .fetch();

        final List<MovieProjectionResponse> result = new ArrayList<>();

        for (final Tuple row : rows) {
            final OffsetDateTime startTime = row.get(projection.startTime);

            if (startTime == null) {
                continue;
            }

            result.add(new MovieProjectionResponse(
                    row.get(projection.id),
                    startTime.atZoneSameInstant(CINEMA_ZONE).toLocalTime(),
                    row.get(venue.id),
                    row.get(venue.name),
                    row.get(city.id),
                    row.get(city.name),
                    row.get(hall.id),
                    row.get(hall.name)
            ));
        }

        return result;
    }

    private List<HeroMovieResponse> mapHeroMovies(final List<UUID> movieIds) {
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

        for (final Tuple row : rows) {
            final UUID movieId = row.get(movie.id);
            if (movieId == null) {
                continue;
            }

            final HeroAccumulator accumulator =
                    accumulatorMap.computeIfAbsent(movieId, ignored -> new HeroAccumulator());

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

        for (final UUID movieId : movieIds) {
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

    private List<MovieCardResponse> mapMovieCards(final List<UUID> movieIds) {
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

        for (final Tuple row : rows) {
            final UUID movieId = row.get(movie.id);
            if (movieId == null) {
                continue;
            }

            final MovieCardAccumulator accumulator =
                    accumulatorMap.computeIfAbsent(movieId, ignored -> new MovieCardAccumulator());

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

        for (final UUID movieId : movieIds) {
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

    private String findCoverImageUrl(final UUID movieId) {
        return queryFactory
                .select(movieImage.imageUrl)
                .from(movieImage)
                .where(movieImage.movie.id.eq(movieId).and(movieImage.isCover.isTrue()))
                .fetchFirst();
    }

    private List<String> findPreviewImageUrls(final UUID movieId) {
        return queryFactory
                .select(movieImage.imageUrl)
                .from(movieImage)
                .where(movieImage.movie.id.eq(movieId).and(movieImage.isCover.isFalse()))
                .orderBy(movieImage.id.asc())
                .limit(PREVIEW_IMAGES_LIMIT)
                .fetch();
    }

    private List<String> findMovieGenres(final UUID movieId) {
        return queryFactory
                .select(genre.name)
                .from(movieGenre)
                .join(movieGenre.genre, genre)
                .where(movieGenre.movie.id.eq(movieId))
                .orderBy(genre.name.asc())
                .fetch();
    }

    private List<MovieCastMemberResponse> findMovieCast(final UUID movieId) {
        final StringExpression fullNameExpression = fullPersonNameExpression();

        final List<Tuple> rows = queryFactory
                .select(
                        fullNameExpression,
                        movieCast.characterName
                )
                .from(movieCast)
                .join(movieCast.person, person)
                .where(movieCast.movie.id.eq(movieId))
                .orderBy(person.lastName.asc(), person.firstName.asc())
                .fetch();

        return rows.stream()
                .map(row -> new MovieCastMemberResponse(
                        row.get(fullNameExpression),
                        row.get(movieCast.characterName)
                ))
                .toList();
    }

    private List<String> findMovieDirectors(final UUID movieId) {
        final StringExpression fullNameExpression = fullPersonNameExpression();

        return queryFactory
                .select(fullNameExpression)
                .from(movieDirector)
                .join(movieDirector.person, person)
                .where(movieDirector.movie.id.eq(movieId))
                .orderBy(person.lastName.asc(), person.firstName.asc())
                .fetch();
    }

    private List<String> findMovieWriters(final UUID movieId) {
        final StringExpression fullNameExpression = fullPersonNameExpression();

        return queryFactory
                .select(fullNameExpression)
                .from(movieWriter)
                .join(movieWriter.person, person)
                .where(movieWriter.movie.id.eq(movieId))
                .orderBy(person.lastName.asc(), person.firstName.asc())
                .fetch();
    }

    private List<MovieDetailsFilterOptionResponse> findMovieCities(final UUID movieId) {
        final List<Tuple> rows = applyProjectionVenueCityGraph(
                queryFactory.select(
                        city.id,
                        city.name
                )
        )
                .where(movie.id.eq(movieId).and(projection.startTime.goe(startOfToday())))
                .groupBy(city.id, city.name)
                .orderBy(city.name.asc())
                .fetch();

        return rows.stream()
                .map(row -> new MovieDetailsFilterOptionResponse(
                        row.get(city.id),
                        row.get(city.name),
                        null
                ))
                .toList();
    }

    private List<MovieDetailsFilterOptionResponse> findMovieVenues(final UUID movieId) {
        final StringExpression venueLabelExpression = venueLabelExpression();
        final List<Tuple> rows = applyProjectionVenueCityGraph(
                queryFactory.select(
                        venue.id,
                        venueLabelExpression,
                        city.id
                )
        )
                .where(movie.id.eq(movieId).and(projection.startTime.goe(startOfToday())))
                .groupBy(venue.id, venue.name, city.id, city.name)
                .orderBy(venue.name.asc(), city.name.asc())
                .fetch();
        return rows.stream()
                .map(row -> new MovieDetailsFilterOptionResponse(
                        row.get(venue.id),
                        row.get(venueLabelExpression),
                        row.get(city.id)
                ))
                .toList();
    }

    private List<LocalDate> findMovieProjectionDates(final UUID movieId) {
        final List<OffsetDateTime> projectionStartTimes = queryFactory
                .select(projection.startTime)
                .from(projection)
                .where(projection.movie.id.eq(movieId).and(projection.startTime.goe(startOfToday())))
                .orderBy(projection.startTime.asc())
                .fetch();

        final Set<LocalDate> dates = new LinkedHashSet<>();

        for (final OffsetDateTime projectionStartTime : projectionStartTimes) {
            dates.add(projectionStartTime.atZoneSameInstant(CINEMA_ZONE).toLocalDate());
        }

        return new ArrayList<>(dates);
    }

    private List<MovieCardResponse> findSeeAlsoMovies(
            final UUID currentMovieId,
            final LocalDate releaseDate,
            final LocalDate endDate
    ) {
        final BooleanExpression seeAlsoPredicate;

        if (isCurrentlyShowingDateRange(releaseDate, endDate)) {
            seeAlsoPredicate = isCurrentlyShowing();
        } else if (isUpcomingDateRange(releaseDate)) {
            seeAlsoPredicate = isUpcoming();
        } else {
            return List.of();
        }

        final List<UUID> movieIds = queryFactory
                .select(movie.id)
                .from(movie)
                .where(seeAlsoPredicate.and(movie.id.ne(currentMovieId)))
                .orderBy(randomOrder())
                .limit(SEE_ALSO_LIMIT)
                .fetch();

        if (movieIds.isEmpty()) {
            return List.of();
        }

        return mapMovieCards(movieIds);
    }

    private BooleanExpression isCurrentlyShowing() {
        final LocalDate today = LocalDate.now(CINEMA_ZONE);

        return movie.status.eq(MovieStatus.PUBLISHED)
                .and(movie.releaseDate.loe(today))
                .and(movie.endDate.isNull().or(movie.endDate.goe(today)));
    }

    private BooleanExpression isUpcoming() {
        return movie.status.eq(MovieStatus.PUBLISHED)
                .and(movie.releaseDate.gt(LocalDate.now(CINEMA_ZONE)));
    }

    private boolean isCurrentlyShowingDateRange(final LocalDate releaseDate, final LocalDate endDate) {
        final LocalDate today = LocalDate.now(CINEMA_ZONE);

        return releaseDate != null
                && !releaseDate.isAfter(today)
                && (endDate == null || !endDate.isBefore(today));
    }

    private boolean isUpcomingDateRange(final LocalDate releaseDate) {
        return releaseDate != null && releaseDate.isAfter(LocalDate.now(CINEMA_ZONE));
    }

    private BooleanExpression isSelectedDate(final LocalDate date) {
        final OffsetDateTime startOfDay = date.atStartOfDay(CINEMA_ZONE).toOffsetDateTime();
        final OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(CINEMA_ZONE).toOffsetDateTime();

        return projection.startTime.goe(startOfDay)
                .and(projection.startTime.lt(endOfDay));
    }

    private OffsetDateTime startOfToday() {
        return LocalDate.now(CINEMA_ZONE)
                .atStartOfDay(CINEMA_ZONE)
                .toOffsetDateTime();
    }

    private <T> JPAQuery<T> applyProjectionVenueCityGraph(final JPAQuery<T> query) {
        return query.from(projection)
                .join(projection.movie, movie)
                .join(projection.hall, hall)
                .join(hall.venue, venue)
                .join(venue.city, city);
    }

    private StringExpression fullPersonNameExpression() {
        return Expressions.stringTemplate(
                "concat({0}, ' ', {1})",
                person.firstName,
                person.lastName
        );
    }

    private StringExpression venueLabelExpression() {
        return Expressions.stringTemplate(
                "concat({0}, ' (', {1}, ')')",
                venue.name,
                city.name
        );
    }

    private OrderSpecifier<Double> randomOrder() {
        return Expressions.numberTemplate(Double.class, "function('random')").asc();
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
