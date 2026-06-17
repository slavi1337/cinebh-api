package com.cinebh.api.dto.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID bookingId,
        UUID movieId,
        UUID projectionId,
        String movieTitle,
        String posterImageUrl,
        String pgRating,
        String language,
        Integer durationMinutes,
        String cityName,
        String venueName,
        String hallName,
        OffsetDateTime projectionStartTime,
        OffsetDateTime expiresAt,
        BigDecimal totalPrice,
        List<SelectedSeatResponse> seats
) {
}
