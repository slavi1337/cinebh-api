package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingQueryRepository {

    Optional<Booking> findLatestByUserProjectionAndStatusForUpdate(
            UUID userId,
            UUID projectionId,
            BookingStatus status
    );

    Optional<Booking> findByIdWithSeats(UUID id);

    List<Booking> findExpiredByStatusForUpdate(BookingStatus status, OffsetDateTime now);
}
