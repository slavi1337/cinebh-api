package com.cinebh.api.dto.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingHoldResponse(
        UUID bookingId,
        UUID projectionId,
        OffsetDateTime expiresAt,
        BigDecimal totalPrice,
        List<SelectedSeatResponse> seats
) {
}
