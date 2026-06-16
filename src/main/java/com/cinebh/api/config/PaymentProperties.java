package com.cinebh.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        Stripe stripe
) {
    public record Stripe(
            String secretKey,
            String webhookSecret,
            String currency
    ) {
    }
}
