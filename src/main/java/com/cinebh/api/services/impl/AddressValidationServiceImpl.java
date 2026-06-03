package com.cinebh.api.services.impl;

import com.cinebh.api.config.NominatimProperties;
import com.cinebh.api.dto.location.NominatimSearchResponse;
import com.cinebh.api.services.AddressValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AddressValidationServiceImpl implements AddressValidationService {
    private static final long NOMINATIM_MIN_DELAY_MILLIS = 1100L;
    private final RestClient nominatimRestClient;
    private final NominatimProperties nominatimProperties;
    private final StringRedisTemplate redisTemplate;
    @Value("${app.validation.cache.address-validation-ttl-days:1}")
    private long redisAddressValidationTtlDays;
    @Value("${app.validation.cache.address-validation-prefix:address_validation:}")
    private String redisAddressValidationPrefix;
    private long lastNominatimRequestAtMillis;

    public AddressValidationServiceImpl(
            @Qualifier("nominatimRestClient") final RestClient nominatimRestClient,
            final NominatimProperties nominatimProperties,
            final StringRedisTemplate redisTemplate
    ) {
        this.nominatimRestClient = nominatimRestClient;
        this.nominatimProperties = nominatimProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isValidStreetInCity(final String city, final String streetAddress) {
        log.info(
                "Nominatim validation called. enabled={}, city={}, streetAddress={}",
                nominatimProperties.enabled(),
                city,
                streetAddress
        );
        if (!nominatimProperties.enabled()) {
            log.warn("Nominatim validation is disabled. Skipping address validation.");
            return true;
        }
        final String streetName = extractStreetName(streetAddress);
        final String normalizedCity = normalize(city);
        final String normalizedStreet = normalize(streetName);
        if (normalizedCity.isBlank() || normalizedStreet.isBlank()) {
            return false;
        }
        final String redisKey = buildRedisKey(normalizedCity, normalizedStreet);
        final Boolean cachedResult = getCachedAddressValidation(redisKey, city, streetName);
        if (cachedResult != null) {
            return cachedResult;
        }
        try {
            final boolean isValid = validateWithNominatim(city, streetName, normalizedCity, normalizedStreet);
            saveAddressValidationToCache(redisKey, isValid, city, streetName);
            return isValid;
        } catch (RestClientException exception) {
            log.warn(
                    "Nominatim validation unavailable for city={} streetName={}. Skipping address validation.",
                    city,
                    streetName,
                    exception
            );
            return true;
        }
    }

    private boolean validateWithNominatim(
            final String city,
            final String streetName,
            final String normalizedCity,
            final String normalizedStreet
    ) {
        final List<NominatimSearchResponse> structuredResults = searchStructured(city, streetName);
        if (hasMatchingResult(structuredResults, normalizedCity, normalizedStreet)) {
            return true;
        }
        final List<NominatimSearchResponse> freeFormResults = searchFreeForm(city, streetName);
        return hasMatchingResult(freeFormResults, normalizedCity, normalizedStreet);
    }

    private Boolean getCachedAddressValidation(
            final String redisKey,
            final String city,
            final String streetName
    ) {
        try {
            final String cachedValue = redisTemplate.opsForValue().get(redisKey);
            if (cachedValue != null) {
                log.info(
                        "CACHE HIT: Address validation for city='{}', street='{}' retrieved from Redis",
                        city,
                        streetName
                );
                return Boolean.parseBoolean(cachedValue);
            }
        } catch (Exception exception) {
            log.warn(
                    "Redis connection failed. Bypassing address validation cache for city='{}', street='{}'",
                    city,
                    streetName,
                    exception
            );
        }
        log.info(
                "CACHE MISS: Calling Nominatim for city='{}', street='{}'",
                city,
                streetName
        );
        return null;
    }

    private void saveAddressValidationToCache(
            final String redisKey,
            final boolean isValid,
            final String city,
            final String streetName
    ) {
        try {
            redisTemplate.opsForValue().set(
                    redisKey,
                    String.valueOf(isValid),
                    redisAddressValidationTtlDays,
                    TimeUnit.DAYS
            );
            log.info(
                    "CACHE SAVED: Address validation for city='{}', street='{}' saved to Redis under key '{}'",
                    city,
                    streetName,
                    redisKey
            );
        } catch (Exception exception) {
            log.warn(
                    "Failed to save address validation result to Redis for city='{}', street='{}'",
                    city,
                    streetName,
                    exception
            );
        }
    }

    private String buildRedisKey(final String normalizedCity, final String normalizedStreet) {
        return redisAddressValidationPrefix + normalizedCity + ":" + normalizedStreet;
    }

    private List<NominatimSearchResponse> searchStructured(
            final String city,
            final String streetName
    ) {
        waitForNominatimTurn();
        final List<NominatimSearchResponse> results = nominatimRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", "1")
                        .queryParam("limit", "5")
                        .queryParam("layer", "address")
                        .queryParam("countrycodes", nominatimProperties.countryCodes())
                        .queryParam("street", streetName)
                        .queryParam("city", city)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        logNominatimResults("structured", results);
        return results == null ? List.of() : results;
    }

    private List<NominatimSearchResponse> searchFreeForm(
            final String city,
            final String streetName
    ) {
        waitForNominatimTurn();
        final List<NominatimSearchResponse> results = nominatimRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", "1")
                        .queryParam("limit", "5")
                        .queryParam("layer", "address")
                        .queryParam("countrycodes", nominatimProperties.countryCodes())
                        .queryParam("q", String.format(
                                "%s, %s, %s",
                                streetName,
                                city,
                                nominatimProperties.countryName()
                        ))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        logNominatimResults("free-form", results);
        return results == null ? List.of() : results;
    }

    private boolean hasMatchingResult(
            final List<NominatimSearchResponse> results,
            final String normalizedCity,
            final String normalizedStreet
    ) {
        return results.stream().anyMatch(result ->
                matchesCountry(result)
                        && matchesCity(result.address(), normalizedCity)
                        && matchesStreet(result.address(), normalizedStreet)
        );
    }

    private synchronized void waitForNominatimTurn() {
        final long now = System.currentTimeMillis();
        final long elapsed = now - lastNominatimRequestAtMillis;
        final long remainingDelay = NOMINATIM_MIN_DELAY_MILLIS - elapsed;
        if (remainingDelay > 0) {
            try {
                Thread.sleep(remainingDelay);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        lastNominatimRequestAtMillis = System.currentTimeMillis();
    }

    private void logNominatimResults(
            final String searchType,
            final List<NominatimSearchResponse> results
    ) {
        log.info(
                "Nominatim {} search returned {} result(s).",
                searchType,
                results == null ? 0 : results.size()
        );
        if (results != null) {
            results.forEach(result ->
                    log.info("Nominatim {} result address={}", searchType, result.address())
            );
        }
    }

    private boolean matchesCountry(final NominatimSearchResponse result) {
        final Map<String, String> address = result.address();
        if (address == null || nominatimProperties.countryCodes() == null) {
            return false;
        }
        final String actualCountryCode = address.getOrDefault("country_code", "");
        return Arrays.stream(nominatimProperties.countryCodes().split(","))
                .map(String::trim)
                .anyMatch(countryCode -> countryCode.equalsIgnoreCase(actualCountryCode));
    }

    private boolean matchesCity(final Map<String, String> address, final String expectedCity) {
        if (address == null) {
            return false;
        }
        return matchesAny(
                address,
                expectedCity,
                "city",
                "town",
                "municipality",
                "village",
                "city_district",
                "county",
                "state_district"
        );
    }

    private boolean matchesStreet(final Map<String, String> address, final String expectedStreet) {
        if (address == null) {
            return false;
        }
        return matchesAny(
                address,
                expectedStreet,
                "road",
                "pedestrian",
                "residential",
                "footway",
                "path",
                "neighbourhood",
                "suburb"
        );
    }

    private boolean matchesAny(
            final Map<String, String> address,
            final String expectedValue,
            final String... keys
    ) {
        for (final String key : keys) {
            final String actualValue = address.get(key);
            if (actualValue == null) {
                continue;
            }
            final String normalizedActualValue = normalize(actualValue);
            if (normalizedActualValue.equals(expectedValue)) {
                return true;
            }
            if (normalizedActualValue.contains(expectedValue)) {
                return true;
            }
            if (expectedValue.contains(normalizedActualValue)) {
                return true;
            }
        }
        return false;
    }

    private String extractStreetName(final String streetAddress) {
        if (streetAddress == null) {
            return "";
        }
        return streetAddress
                .replaceAll("(?i)\\b\\d+[a-z]?\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalize(final String value) {
        if (value == null) {
            return "";
        }
        final String withoutDiacritics = Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replace("đ", "dj")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
