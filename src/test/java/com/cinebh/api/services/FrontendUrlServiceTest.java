package com.cinebh.api.services;

import com.cinebh.api.config.FrontendProperties;
import com.cinebh.api.services.impl.FrontendUrlService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendUrlServiceTest {

    private final FrontendUrlService frontendUrlService = new FrontendUrlService(
            new FrontendProperties("https://cinebh.test/")
    );

    @Test
    void shouldBuildCheckoutSuccessUrlWithStripeSessionPlaceholder() {
        final String url = frontendUrlService.checkoutSuccessUrl(
                UUID.fromString("00000000-0000-0000-0000-000000000111")
        );

        assertThat(url).isEqualTo(
                "https://cinebh.test/checkout/success?session_id={CHECKOUT_SESSION_ID}" +
                        "&ticketCode=00000000-0000-0000-0000-000000000111"
        );
    }

    @Test
    void shouldBuildCheckoutCancelUrl() {
        final String url = frontendUrlService.checkoutCancelUrl(
                UUID.fromString("00000000-0000-0000-0000-000000000222"),
                UUID.fromString("00000000-0000-0000-0000-000000000333")
        );

        assertThat(url).isEqualTo(
                "https://cinebh.test/movies/00000000-0000-0000-0000-000000000222/seats" +
                        "?projectionId=00000000-0000-0000-0000-000000000333&mode=buy&payment=cancelled"
        );
    }

    @Test
    void shouldBuildReservationCheckoutCancelUrl() {
        final String url = frontendUrlService.reservationCheckoutCancelUrl();

        assertThat(url).isEqualTo(
                "https://cinebh.test/profile/reservations?payment=cancelled"
        );
    }

    @Test
    void shouldBuildTicketConfirmationUrl() {
        final String url = frontendUrlService.ticketConfirmationUrl(
                UUID.fromString("00000000-0000-0000-0000-000000000111")
        );

        assertThat(url).isEqualTo(
                "https://cinebh.test/tickets/confirmation?ticketCode=00000000-0000-0000-0000-000000000111"
        );
    }
}
