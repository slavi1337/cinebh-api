package com.cinebh.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        @Valid
        @NotNull
        Stripe stripe
) {
    public record Stripe(
            @NotBlank
            String secretKey,

            @NotBlank
            String webhookSecret,

            String currency
    ) {
    }
}
