package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.entities.QCity;
import com.cinebh.api.entities.QVenue;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VenueQueryRepositoryImpl implements VenueQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QVenue venue = QVenue.venue;
    private final QCity city = QCity.city;

    @Override
    public PageResponse<VenueCardResponse> findHomepageVenues(int page, int size) {
        final long totalElements = Optional.ofNullable(
                queryFactory
                        .select(venue.count())
                        .from(venue)
                        .fetchOne()
        ).orElse(0L);

        final List<VenueCardResponse> items = queryFactory
                .select(
                        Projections.constructor(
                                VenueCardResponse.class,
                                venue.id,
                                venue.name,
                                Expressions.stringTemplate(
                                        "concat({0}, ', ', {1})",
                                        venue.streetAddress,
                                        city.name
                                ),
                                venue.imageUrl
                        )
                )
                .from(venue)
                .join(venue.city, city)
                .orderBy(venue.name.asc())
                .offset((long) page * size)
                .limit(size)
                .fetch();

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                calculateTotalPages(totalElements, size)
        );
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (size <= 0) {
            return 0;
        }

        return (int) Math.ceil((double) totalElements / size);
    }
}
