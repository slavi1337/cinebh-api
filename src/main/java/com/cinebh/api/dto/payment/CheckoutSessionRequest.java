package com.cinebh.api.dto.payment;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutSessionRequest(
        @NotNull
        UUID bookingId
) {
}
