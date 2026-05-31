package com.cinebh.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.nominatim")
public record NominatimProperties(
        boolean enabled,
        String baseUrl,
        String userAgent,
        String countryCodes,
        String countryName
) {
}
