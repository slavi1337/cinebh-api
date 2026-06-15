package com.cinebh.api.dto.booking;

import com.cinebh.api.entities.enums.SeatType;

import java.math.BigDecimal;
import java.util.UUID;

public record SeatResponse(
        UUID id,
        String row,
        String number,
        SeatType type,
        BigDecimal price,
        SeatAvailabilityStatus status,
        boolean selectedByCurrentUser
) {
}
