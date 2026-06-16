package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.QBooking;
import com.cinebh.api.entities.QBookingSeat;
import com.cinebh.api.entities.QCity;
import com.cinebh.api.entities.QHall;
import com.cinebh.api.entities.QMovie;
import com.cinebh.api.entities.QProjection;
import com.cinebh.api.entities.QSeatTemplate;
import com.cinebh.api.entities.QUser;
import com.cinebh.api.entities.QVenue;
import com.cinebh.api.entities.enums.BookingStatus;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BookingQueryRepositoryImpl implements BookingQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QBooking booking = QBooking.booking;
    private final QBookingSeat bookingSeat = QBookingSeat.bookingSeat;
    private final QSeatTemplate seatTemplate = QSeatTemplate.seatTemplate;
    private final QProjection projection = QProjection.projection;
    private final QUser user = QUser.user;
    private final QMovie movie = QMovie.movie;
    private final QHall hall = QHall.hall;
    private final QVenue venue = QVenue.venue;
    private final QCity city = QCity.city;

    @Override
    public Optional<Booking> findLatestByUserProjectionAndStatusForUpdate(
            final UUID userId,
            final UUID projectionId,
            final BookingStatus status
    ) {
        return applyBookingGraph(selectDistinctBooking())
                .where(booking.user.id.eq(userId)
                        .and(booking.projection.id.eq(projectionId))
                        .and(booking.status.eq(status)))
                .orderBy(booking.createdAt.desc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Booking> findByIdWithSeats(final UUID id) {
        return Optional.ofNullable(
                applyBookingGraph(selectDistinctBooking())
                        .where(booking.id.eq(id))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Booking> findByIdWithPaymentDetailsForUpdate(final UUID id) {
        return Optional.ofNullable(
                applyPaymentDetailsGraph(selectDistinctBooking())
                        .where(booking.id.eq(id))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public Optional<Booking> findByTicketCodeWithPaymentDetails(final UUID ticketCode) {
        return Optional.ofNullable(
                applyPaymentDetailsGraph(selectDistinctBooking())
                        .where(booking.ticketCode.eq(ticketCode))
                        .fetchOne()
        );
    }

    @Override
    public List<Booking> findExpiredByStatusForUpdate(final BookingStatus status, final OffsetDateTime now) {
        return applyBookingGraph(selectDistinctBooking())
                .where(booking.status.eq(status)
                        .and(booking.expiresAt.loe(now)))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    private JPAQuery<Booking> selectDistinctBooking() {
        return queryFactory
                .select(booking)
                .distinct()
                .from(booking);
    }

    private JPAQuery<Booking> applyBookingGraph(final JPAQuery<Booking> query) {
        return query
                .leftJoin(booking.projection, projection).fetchJoin()
                .leftJoin(booking.seats, bookingSeat).fetchJoin()
                .leftJoin(bookingSeat.seatTemplate, seatTemplate).fetchJoin();
    }

    private JPAQuery<Booking> applyPaymentDetailsGraph(final JPAQuery<Booking> query) {
        return applyBookingGraph(query)
                .leftJoin(booking.user, user).fetchJoin()
                .leftJoin(projection.movie, movie).fetchJoin()
                .leftJoin(projection.hall, hall).fetchJoin()
                .leftJoin(hall.venue, venue).fetchJoin()
                .leftJoin(venue.city, city).fetchJoin();
    }
}
