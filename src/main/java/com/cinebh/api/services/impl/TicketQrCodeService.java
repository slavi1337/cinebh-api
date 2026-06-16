package com.cinebh.api.services.impl;

import com.cinebh.api.exceptions.ApiException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketQrCodeService {

    private static final Logger log = LoggerFactory.getLogger(TicketQrCodeService.class);
    private static final int TICKET_QR_SIZE_PX = 240;

    private final FrontendUrlService frontendUrlService;

    public byte[] createTicketQrCode(final UUID ticketCode) {
        if (ticketCode == null) {
            return null;
        }

        final String ticketUrl = ticketConfirmationUrl(ticketCode);
        if (ticketUrl == null) {
            return null;
        }

        try {
            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    qrCodeWriter.encode(
                            ticketUrl,
                            BarcodeFormat.QR_CODE,
                            TICKET_QR_SIZE_PX,
                            TICKET_QR_SIZE_PX
                    ),
                    "PNG",
                    outputStream
            );

            return outputStream.toByteArray();
        } catch (WriterException | IOException exception) {
            log.error("Failed to generate ticket QR code for ticketCode={}", ticketCode, exception);
            return null;
        }
    }

    private String ticketConfirmationUrl(final UUID ticketCode) {
        try {
            return frontendUrlService.ticketConfirmationUrl(ticketCode);
        } catch (ApiException exception) {
            log.warn(
                    "Ticket URL could not be generated for ticketCode={}: {}",
                    ticketCode,
                    exception.getMessage()
            );
            return null;
        } catch (RuntimeException exception) {
            log.error("Ticket URL could not be generated for ticketCode={}", ticketCode, exception);
            return null;
        }
    }
}
