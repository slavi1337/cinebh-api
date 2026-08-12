package com.cinebh.api.services;

import com.cinebh.api.config.FrontendProperties;
import com.cinebh.api.services.impl.FrontendUrlService;
import com.cinebh.api.services.impl.TicketQrCodeService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketQrCodeServiceTest {

    @Test
    void shouldCreateTicketQrCode() {
        final TicketQrCodeService ticketQrCodeService = new TicketQrCodeService(
                new FrontendUrlService(new FrontendProperties("https://cinebh.test"))
        );

        final byte[] qrCode = ticketQrCodeService.createTicketQrCode(
                UUID.fromString("00000000-0000-0000-0000-000000000111")
        );

        assertThat(qrCode).isNotEmpty();
    }

    @Test
    void shouldReturnNullWhenFrontendBaseUrlIsMissing() {
        final TicketQrCodeService ticketQrCodeService = new TicketQrCodeService(
                new FrontendUrlService(new FrontendProperties(""))
        );

        final byte[] qrCode = ticketQrCodeService.createTicketQrCode(
                UUID.fromString("00000000-0000-0000-0000-000000000111")
        );

        assertThat(qrCode).isNull();
    }
}
