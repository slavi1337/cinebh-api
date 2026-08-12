package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentQueryRepository {

    Optional<Payment> findByStripeSessionIdWithBooking(String stripeSessionId);

    Optional<Payment> findSuccessfulByBookingId(UUID bookingId);
}
