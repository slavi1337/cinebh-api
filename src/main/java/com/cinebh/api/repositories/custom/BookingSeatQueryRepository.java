package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.BookingSeat;
import com.cinebh.api.entities.enums.BookingStatus;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingSeatQueryRepository {

    List<BookingSeat> findActiveSeatsForProjection(
            UUID projectionId,
            Collection<BookingStatus> statuses
    );

    List<UUID> findUnavailableSeatTemplateIds(
            UUID projectionId,
            Collection<UUID> seatTemplateIds,
            UUID bookingId
    );
}
