package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BookingQueryRepository {

    Optional<Booking> findLatestByUserProjectionAndStatus(
            UUID userId,
            UUID projectionId,
            BookingStatus status
    );

    Optional<Booking> findLatestByUserProjectionAndStatusForUpdate(
            UUID userId,
            UUID projectionId,
            BookingStatus status
    );

    Optional<Booking> findByIdWithSeats(UUID id);

    Optional<Booking> findByIdWithPaymentDetailsForUpdate(UUID id);

    Optional<Booking> findByIdWithDetailsForUpdate(UUID id);

    Optional<Booking> findByTicketCodeWithPaymentDetails(UUID ticketCode);

    List<Booking> findReservationsByUserId(UUID userId, OffsetDateTime now);

    List<Booking> findPaidBookingsByUserId(UUID userId, OffsetDateTime now, boolean upcoming);

    Map<UUID, String> findCoverImageUrlsByMovieIds(Collection<UUID> movieIds);

    List<Booking> findExpiredByStatusesForUpdate(Collection<BookingStatus> statuses, OffsetDateTime now);
}
