package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.Payment;
import com.cinebh.api.entities.QBooking;
import com.cinebh.api.entities.QBookingSeat;
import com.cinebh.api.entities.QCity;
import com.cinebh.api.entities.QHall;
import com.cinebh.api.entities.QMovie;
import com.cinebh.api.entities.QPayment;
import com.cinebh.api.entities.QProjection;
import com.cinebh.api.entities.QSeatTemplate;
import com.cinebh.api.entities.QUser;
import com.cinebh.api.entities.QVenue;
import com.cinebh.api.entities.enums.PaymentStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentQueryRepositoryImpl implements PaymentQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QPayment payment = QPayment.payment;
    private final QBooking booking = QBooking.booking;
    private final QBookingSeat bookingSeat = QBookingSeat.bookingSeat;
    private final QSeatTemplate seatTemplate = QSeatTemplate.seatTemplate;
    private final QUser user = QUser.user;
    private final QProjection projection = QProjection.projection;
    private final QMovie movie = QMovie.movie;
    private final QHall hall = QHall.hall;
    private final QVenue venue = QVenue.venue;
    private final QCity city = QCity.city;

    @Override
    public Optional<Payment> findByStripeSessionIdWithBooking(final String stripeSessionId) {
        return Optional.ofNullable(
                queryFactory
                        .select(payment)
                        .distinct()
                        .from(payment)
                        .join(payment.booking, booking).fetchJoin()
                        .join(booking.user, user).fetchJoin()
                        .join(booking.projection, projection).fetchJoin()
                        .join(projection.movie, movie).fetchJoin()
                        .join(projection.hall, hall).fetchJoin()
                        .join(hall.venue, venue).fetchJoin()
                        .join(venue.city, city).fetchJoin()
                        .leftJoin(booking.seats, bookingSeat).fetchJoin()
                        .leftJoin(bookingSeat.seatTemplate, seatTemplate).fetchJoin()
                        .where(payment.stripeSessionId.eq(stripeSessionId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public Optional<Payment> findSuccessfulByBookingId(final UUID bookingId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(payment)
                        .where(payment.booking.id.eq(bookingId)
                                .and(payment.status.eq(PaymentStatus.SUCCESS)))
                        .orderBy(payment.paidAt.desc(), payment.createdAt.desc())
                        .fetchFirst()
        );
    }
}
