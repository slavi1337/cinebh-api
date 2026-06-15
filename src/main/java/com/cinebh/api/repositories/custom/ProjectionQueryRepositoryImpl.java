package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.Projection;
import com.cinebh.api.entities.QCity;
import com.cinebh.api.entities.QHall;
import com.cinebh.api.entities.QMovie;
import com.cinebh.api.entities.QProjection;
import com.cinebh.api.entities.QVenue;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProjectionQueryRepositoryImpl implements ProjectionQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QProjection projection = QProjection.projection;
    private final QMovie movie = QMovie.movie;
    private final QHall hall = QHall.hall;
    private final QVenue venue = QVenue.venue;
    private final QCity city = QCity.city;

    @Override
    public Optional<Projection> findByIdWithDetails(final UUID id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(projection)
                        .join(projection.movie, movie).fetchJoin()
                        .join(projection.hall, hall).fetchJoin()
                        .join(hall.venue, venue).fetchJoin()
                        .join(venue.city, city).fetchJoin()
                        .where(projection.id.eq(id))
                        .fetchOne()
        );
    }
}
