package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.BookingSeat;
import com.cinebh.api.entities.QBooking;
import com.cinebh.api.entities.QBookingSeat;
import com.cinebh.api.entities.QSeatTemplate;
import com.cinebh.api.entities.QUser;
import com.cinebh.api.entities.enums.BookingStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BookingSeatQueryRepositoryImpl implements BookingSeatQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QBookingSeat bookingSeat = QBookingSeat.bookingSeat;
    private final QBooking booking = QBooking.booking;
    private final QSeatTemplate seatTemplate = QSeatTemplate.seatTemplate;
    private final QUser user = QUser.user;

    @Override
    public List<BookingSeat> findActiveSeatsForProjection(
            final UUID projectionId,
            final Collection<BookingStatus> statuses
    ) {
        return queryFactory
                .selectFrom(bookingSeat)
                .join(bookingSeat.booking, booking).fetchJoin()
                .join(bookingSeat.seatTemplate, seatTemplate).fetchJoin()
                .join(booking.user, user).fetchJoin()
                .where(isActiveForProjection(projectionId)
                        .and(booking.status.in(statuses)))
                .fetch();
    }

    @Override
    public List<UUID> findUnavailableSeatTemplateIds(
            final UUID projectionId,
            final Collection<UUID> seatTemplateIds,
            final UUID bookingId
    ) {
        return queryFactory
                .select(bookingSeat.seatTemplate.id)
                .from(bookingSeat)
                .where(isActiveForProjection(projectionId)
                        .and(bookingSeat.booking.id.ne(bookingId))
                        .and(bookingSeat.seatTemplate.id.in(seatTemplateIds)))
                .fetch();
    }

    private BooleanExpression isActiveForProjection(final UUID projectionId) {
        return bookingSeat.projection.id.eq(projectionId)
                .and(bookingSeat.active.isTrue());
    }
}
