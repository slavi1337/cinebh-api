package com.cinebh.api.services;

import com.cinebh.api.config.NominatimProperties;
import com.cinebh.api.dto.location.NominatimSearchResponse;
import com.cinebh.api.services.impl.AddressValidationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AddressValidationServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void shouldSkipAddressValidationWhenNominatimIsDisabled() {
        final AddressValidationServiceImpl service = createService(false, "ba");

        final boolean isValid = service.isValidStreetInCity("Sarajevo", "Ferhadija 1");

        assertThat(isValid).isTrue();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldSkipAddressValidationWithoutCachingWhenNominatimIsUnavailable() {
        final ServiceWithServer serviceWithServer = createServiceWithServer("ba", "Bosnia and Herzegovina");
        final AddressValidationServiceImpl service = serviceWithServer.service();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        serviceWithServer.server().expect(requestTo(
                        "https://nominatim.test/search?format=jsonv2&addressdetails=1&limit=5&layer=address&countrycodes=ba&street=Ferhadija&city=Sarajevo"
                ))
                .andRespond(withException(new IOException("Timeout")));

        final boolean isValid = service.isValidStreetInCity("Sarajevo", "Ferhadija 1");

        assertThat(isValid).isTrue();
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        serviceWithServer.server().verify();
    }

    @Test
    void shouldReturnCachedAddressValidation() {
        final AddressValidationServiceImpl service = createService(true, "ba");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("address_validation:sarajevo:ferhadija")).thenReturn("false");

        final boolean isValid = service.isValidStreetInCity("Sarajevo", "Ferhadija 1");

        assertThat(isValid).isFalse();
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldCacheValidStructuredNominatimResult() {
        final ServiceWithServer serviceWithServer = createServiceWithServer("ba", "Bosnia and Herzegovina");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andExpect(queryParam("street", "Ferhadija"))
                .andExpect(queryParam("city", "Sarajevo"))
                .andRespond(withSuccess(
                        """
                                [{"address":{"country_code":"ba","city":"Sarajevo","road":"Ferhadija"}}]
                                """,
                        MediaType.APPLICATION_JSON
                ));

        final boolean isValid = serviceWithServer.service()
                .isValidStreetInCity("Sarajevo", "Ferhadija 1");

        assertThat(isValid).isTrue();
        verify(valueOperations).set("address_validation:sarajevo:ferhadija", "true", 1L, TimeUnit.DAYS);
        serviceWithServer.server().verify();
    }

    @Test
    void shouldUseConfiguredCountryNameForFreeFormSearch() {
        final ServiceWithServer serviceWithServer = createServiceWithServer("hr", "Croatia");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andExpect(queryParam("street", "Ilica"))
                .andExpect(queryParam("city", "Zagreb"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andExpect(queryParam("q", "Ilica,%20Zagreb,%20Croatia"))
                .andRespond(withSuccess(
                        """
                                [{"address":{"country_code":"hr","city":"Zagreb","road":"Ilica"}}]
                                """,
                        MediaType.APPLICATION_JSON
                ));

        final boolean isValid = serviceWithServer.service()
                .isValidStreetInCity("Zagreb", "Ilica 1");

        assertThat(isValid).isTrue();
        verify(valueOperations).set("address_validation:zagreb:ilica", "true", 1L, TimeUnit.DAYS);
        serviceWithServer.server().verify();
    }

    @Test
    void shouldCacheInvalidAddressWhenNominatimReturnsNoMatches() {
        final ServiceWithServer serviceWithServer = createServiceWithServer("ba", "Bosnia and Herzegovina");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        final boolean isValid = serviceWithServer.service()
                .isValidStreetInCity("Sarajevo", "Nepostojeca 1");

        assertThat(isValid).isFalse();
        verify(valueOperations).set("address_validation:sarajevo:nepostojeca", "false", 1L, TimeUnit.DAYS);
        serviceWithServer.server().verify();
    }

    @Test
    void shouldRejectStreetWhenNominatimReturnsDifferentCity() {
        final ServiceWithServer serviceWithServer = createServiceWithServer("ba", "Bosnia and Herzegovina");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess(
                        """
                                [{"address":{"country_code":"ba","city":"Mostar","road":"Ferhadija"}}]
                                """,
                        MediaType.APPLICATION_JSON
                ));
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        final boolean isValid = serviceWithServer.service()
                .isValidStreetInCity("Sarajevo", "Ferhadija 1");

        assertThat(isValid).isFalse();
        verify(valueOperations).set("address_validation:sarajevo:ferhadija", "false", 1L, TimeUnit.DAYS);
        serviceWithServer.server().verify();
    }

    @Test
    void shouldContinueValidationWhenRedisIsUnavailable() {
        final ServiceWithServer serviceWithServer = createServiceWithServer("ba", "Bosnia and Herzegovina");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("address_validation:sarajevo:ferhadija"))
                .thenThrow(new RuntimeException("Redis unavailable"));
        doThrow(new RuntimeException("Redis unavailable"))
                .when(valueOperations)
                .set("address_validation:sarajevo:ferhadija", "true", 1L, TimeUnit.DAYS);
        serviceWithServer.server()
                .expect(requestTo(startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess(
                        """
                                [{"address":{"country_code":"ba","city":"Sarajevo","road":"Ferhadija"}}]
                                """,
                        MediaType.APPLICATION_JSON
                ));

        final boolean isValid = serviceWithServer.service()
                .isValidStreetInCity("Sarajevo", "Ferhadija 1");

        assertThat(isValid).isTrue();
        serviceWithServer.server().verify();
    }

    @Test
    void shouldRejectBlankAddressData() {
        final AddressValidationServiceImpl service = createService(true, "ba");

        final boolean isValid = service.isValidStreetInCity(null, null);

        assertThat(isValid).isFalse();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldMatchAnyConfiguredCountryCode() {
        final AddressValidationServiceImpl service = createService(true, "hr, ba");
        final NominatimSearchResponse result = new NominatimSearchResponse(
                null,
                null,
                null,
                null,
                Map.of("country_code", "ba")
        );

        final Boolean matchesCountry = ReflectionTestUtils.invokeMethod(
                service,
                "matchesCountry",
                result
        );

        assertThat(matchesCountry).isTrue();
    }

    private AddressValidationServiceImpl createService(
            final boolean enabled,
            final String countryCodes
    ) {
        return createService(
                RestClient.create(),
                enabled,
                countryCodes,
                "Bosnia and Herzegovina"
        );
    }

    private AddressValidationServiceImpl createService(
            final RestClient restClient,
            final boolean enabled,
            final String countryCodes,
            final String countryName
    ) {
        final NominatimProperties properties = new NominatimProperties(
                enabled,
                "https://nominatim.test",
                "CineBHTest",
                countryCodes,
                countryName
        );
        final AddressValidationServiceImpl service = new AddressValidationServiceImpl(
                restClient,
                properties,
                redisTemplate
        );
        ReflectionTestUtils.setField(service, "redisAddressValidationPrefix", "address_validation:");
        ReflectionTestUtils.setField(service, "redisAddressValidationTtlDays", 1L);
        return service;
    }

    private ServiceWithServer createServiceWithServer(
            final String countryCodes,
            final String countryName
    ) {
        final RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://nominatim.test");
        final MockRestServiceServer server = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();
        return new ServiceWithServer(
                createService(restClientBuilder.build(), true, countryCodes, countryName),
                server
        );
    }

    private record ServiceWithServer(
            AddressValidationServiceImpl service,
            MockRestServiceServer server
    ) {
    }
}
