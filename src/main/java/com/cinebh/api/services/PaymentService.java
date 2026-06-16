package com.cinebh.api.services;

import com.cinebh.api.dto.payment.CheckoutSessionRequest;
import com.cinebh.api.dto.payment.CheckoutSessionResponse;

public interface PaymentService {

    CheckoutSessionResponse createCheckoutSession(CheckoutSessionRequest request);

    void handleStripeWebhook(String payload, String signatureHeader);
}
