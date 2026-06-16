package com.cinebh.api.dto.ticket;

public record TicketValidationResponse(
        boolean valid,
        TicketValidationStatus status,
        String message,
        TicketDetailsResponse ticket
) {
}
