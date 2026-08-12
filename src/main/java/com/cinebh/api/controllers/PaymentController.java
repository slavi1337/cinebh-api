package com.cinebh.api.controllers;

import com.cinebh.api.dto.payment.CheckoutSessionRequest;
import com.cinebh.api.dto.payment.CheckoutSessionResponse;
import com.cinebh.api.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Authenticated checkout and Stripe webhook endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout-sessions")
    @Operation(
            summary = "Create Stripe Checkout Session",
            description = "Creates a Stripe-hosted checkout session for the current user's active booking hold"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout session created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired booking hold"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "Booking hold not found"),
            @ApiResponse(responseCode = "502", description = "Stripe checkout session could not be created")
    })
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody final CheckoutSessionRequest request
    ) {
        return ResponseEntity.ok(paymentService.createCheckoutSession(request));
    }

    @PostMapping("/stripe/webhook")
    @Operation(
            summary = "Handle Stripe webhook",
            description = "Confirms successful Stripe Checkout payments and marks bookings as paid"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Stripe webhook payload or signature")
    })
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody final String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) final String signatureHeader
    ) {
        paymentService.handleStripeWebhook(payload, signatureHeader);
        return ResponseEntity.noContent().build();
    }
}
