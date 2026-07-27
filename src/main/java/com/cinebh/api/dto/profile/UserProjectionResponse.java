package com.cinebh.api.dto.profile;

import com.cinebh.api.dto.booking.SelectedSeatResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserProjectionResponse(
        UUID bookingId,
        UUID ticketCode,
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
        BigDecimal totalPrice,
        List<SelectedSeatResponse> seats
) {
}
