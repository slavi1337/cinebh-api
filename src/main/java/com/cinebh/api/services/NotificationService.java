package com.cinebh.api.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void sendAccountVerificationCode(String toEmail, String toName, String code);

    void sendPasswordResetCode(String toEmail, String toName, String code);

    void sendTicketPurchaseConfirmation(
            String toEmail,
            String toName,
            UUID bookingId,
            UUID ticketCode,
            String movieTitle,
            String cityName,
            String venueName,
            String hallName,
            OffsetDateTime projectionStartTime,
            List<String> seats,
            BigDecimal totalPrice,
            String currency
    );
}
