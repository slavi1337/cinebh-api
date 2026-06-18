package com.cinebh.api.dto.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BookingHoldRequest(
        @NotNull
        UUID projectionId,

        @NotEmpty
        List<@NotNull UUID> seatTemplateIds
) {
}
