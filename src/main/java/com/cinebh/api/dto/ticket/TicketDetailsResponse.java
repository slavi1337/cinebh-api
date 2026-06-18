package com.cinebh.api.dto.ticket;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TicketDetailsResponse(
        UUID bookingId,
        UUID ticketCode,
        String movieTitle,
        String cityName,
        String venueName,
        String hallName,
        OffsetDateTime projectionStartTime,
        List<String> seats,
        BigDecimal totalPaid,
        String currency
) {
}
