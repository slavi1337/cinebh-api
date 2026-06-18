package com.cinebh.api.services.impl;

import com.cinebh.api.dto.ticket.TicketDetailsResponse;
import com.cinebh.api.dto.ticket.TicketValidationResponse;
import com.cinebh.api.dto.ticket.TicketValidationStatus;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.Payment;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.PaymentRepository;
import com.cinebh.api.services.TicketService;
import com.cinebh.api.utils.BookingSeatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public TicketValidationResponse validateTicket(final UUID ticketCode) {
        return bookingRepository.findByTicketCodeWithPaymentDetails(ticketCode)
                .map(this::validateBooking)
                .orElseGet(() -> invalid("Ticket was not found."));
    }

    private TicketValidationResponse validateBooking(final Booking booking) {
        return switch (booking.getStatus()) {
            case PAID -> validatePaidBooking(booking);
            case HOLD, RESERVED -> pending("Ticket payment is still being confirmed.");
            case CANCELLED -> invalid("Ticket booking has been cancelled.");
            case EXPIRED -> invalid("Ticket booking has expired.");
        };
    }

    private TicketValidationResponse validatePaidBooking(final Booking booking) {
        return paymentRepository.findSuccessfulByBookingId(booking.getId())
                .map(payment -> valid(booking, payment))
                .orElseGet(() -> invalid("Ticket payment could not be verified."));
    }

    private TicketValidationResponse valid(final Booking booking, final Payment payment) {
        return new TicketValidationResponse(
                true,
                TicketValidationStatus.VALID,
                "Ticket is valid.",
                new TicketDetailsResponse(
                        booking.getId(),
                        booking.getTicketCode(),
                        booking.getProjection().getMovie().getTitle(),
                        booking.getProjection().getHall().getVenue().getCity().getName(),
                        booking.getProjection().getHall().getVenue().getName(),
                        booking.getProjection().getHall().getName(),
                        booking.getProjection().getStartTime(),
                        BookingSeatUtils.activeSeatLabels(booking),
                        payment.getAmount(),
                        payment.getCurrency().toUpperCase(Locale.ROOT)
                )
        );
    }

    private TicketValidationResponse pending(final String message) {
        return new TicketValidationResponse(false, TicketValidationStatus.PENDING, message, null);
    }

    private TicketValidationResponse invalid(final String message) {
        return new TicketValidationResponse(false, TicketValidationStatus.INVALID, message, null);
    }
}
