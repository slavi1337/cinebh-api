package com.cinebh.api.services.impl;

import com.cinebh.api.config.FrontendProperties;
import com.cinebh.api.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FrontendUrlService {

    private static final String CHECKOUT_SUCCESS_PATH = "/checkout/success";
    private static final String TICKET_CONFIRMATION_PATH = "/tickets/confirmation";

    private final FrontendProperties frontendProperties;

    public String checkoutSuccessUrl(final UUID ticketCode) {
        return normalizedFrontendBaseUrl()
                + CHECKOUT_SUCCESS_PATH
                + "?session_id={CHECKOUT_SESSION_ID}&ticketCode="
                + ticketCode;
    }

    public String checkoutCancelUrl(final UUID movieId, final UUID projectionId) {
        return UriComponentsBuilder
                .fromUriString(normalizedFrontendBaseUrl())
                .path("/movies/{movieId}/seats")
                .queryParam("projectionId", projectionId)
                .queryParam("mode", "buy")
                .queryParam("payment", "cancelled")
                .build(movieId)
                .toString();
    }

    public String ticketConfirmationUrl(final UUID ticketCode) {
        return UriComponentsBuilder
                .fromUriString(normalizedFrontendBaseUrl())
                .path(TICKET_CONFIRMATION_PATH)
                .queryParam("ticketCode", ticketCode)
                .build()
                .encode()
                .toUriString();
    }

    private String normalizedFrontendBaseUrl() {
        final String baseUrl = frontendProperties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApiException("Frontend base URL is not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return baseUrl.replaceAll("/$", "");
    }
}
