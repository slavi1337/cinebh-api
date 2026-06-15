package com.cinebh.api.dto.booking;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BookingHoldRequest(
        @NotNull
        UUID projectionId,

        @NotNull
        List<@NotNull UUID> seatTemplateIds
) {
}
