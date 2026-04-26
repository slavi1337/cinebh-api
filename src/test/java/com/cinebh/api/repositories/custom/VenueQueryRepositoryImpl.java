package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueQueryRepositoryImplTest {

    @Test
    void shouldReturnHomepageVenuesPage() {
        final JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);

        final JPAQuery countQuery = mock(JPAQuery.class, RETURNS_SELF);
        final JPAQuery itemsQuery = mock(JPAQuery.class, RETURNS_SELF);

        final AtomicInteger selectCallCounter = new AtomicInteger();

        when(queryFactory.select(any(Expression.class))).thenAnswer(invocation ->
                selectCallCounter.getAndIncrement() == 0 ? countQuery : itemsQuery
        );

        final VenueCardResponse venueCardResponse = new VenueCardResponse(
                UUID.randomUUID(),
                "Cinema City",
                "Zmaja od Bosne, Sarajevo",
                "https://example.com/venue.jpg"
        );

        when(countQuery.fetchOne()).thenReturn(1L);
        when(itemsQuery.fetch()).thenReturn(List.of(venueCardResponse));

        final VenueQueryRepositoryImpl repository = new VenueQueryRepositoryImpl(queryFactory);

        final PageResponse<VenueCardResponse> response = repository.findHomepageVenues(0, 10);

        assertThat(response.items()).containsExactly(venueCardResponse);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyHomepageVenuesPageWhenNoVenuesExist() {
        final JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);

        final JPAQuery countQuery = mock(JPAQuery.class, RETURNS_SELF);
        final JPAQuery itemsQuery = mock(JPAQuery.class, RETURNS_SELF);

        final AtomicInteger selectCallCounter = new AtomicInteger();

        when(queryFactory.select(any(Expression.class))).thenAnswer(invocation ->
                selectCallCounter.getAndIncrement() == 0 ? countQuery : itemsQuery
        );

        when(countQuery.fetchOne()).thenReturn(0L);
        when(itemsQuery.fetch()).thenReturn(List.of());

        final VenueQueryRepositoryImpl repository = new VenueQueryRepositoryImpl(queryFactory);

        final PageResponse<VenueCardResponse> response = repository.findHomepageVenues(1, 10);

        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void shouldTreatNullCountAsZero() {
        final JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);

        final JPAQuery countQuery = mock(JPAQuery.class, RETURNS_SELF);
        final JPAQuery itemsQuery = mock(JPAQuery.class, RETURNS_SELF);

        final AtomicInteger selectCallCounter = new AtomicInteger();

        when(queryFactory.select(any(Expression.class))).thenAnswer(invocation ->
                selectCallCounter.getAndIncrement() == 0 ? countQuery : itemsQuery
        );

        when(countQuery.fetchOne()).thenReturn(null);
        when(itemsQuery.fetch()).thenReturn(List.of());

        final VenueQueryRepositoryImpl repository = new VenueQueryRepositoryImpl(queryFactory);

        final PageResponse<VenueCardResponse> response = repository.findHomepageVenues(0, 10);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
