package com.cinebh.api.dto.booking;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatMapResponse(
        UUID projectionId,
        UUID movieId,
        String movieTitle,
        String cityName,
        String venueName,
        String hallName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<SeatResponse> seats,
        BookingHoldResponse activeHold
) {
}
