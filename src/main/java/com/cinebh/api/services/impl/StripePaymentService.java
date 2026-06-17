package com.cinebh.api.services.impl;

import com.cinebh.api.config.PaymentProperties;
import com.cinebh.api.dto.payment.CheckoutSessionRequest;
import com.cinebh.api.dto.payment.CheckoutSessionResponse;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.Payment;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.entities.enums.PaymentStatus;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.PaymentRepository;
import com.cinebh.api.services.NotificationService;
import com.cinebh.api.services.PaymentService;
import com.cinebh.api.utils.BookingSessionDurations;
import com.cinebh.api.utils.BookingSeatUtils;
import com.cinebh.api.utils.SecurityUtils;
import com.cinebh.api.websocket.ProjectionSeatEventPublisher;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StripePaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);
    private static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
    private static final String PAID_PAYMENT_STATUS = "paid";
    private static final String BOOKING_ID_METADATA_KEY = "bookingId";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProperties paymentProperties;
    private final FrontendUrlService frontendUrlService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final ProjectionSeatEventPublisher projectionSeatEventPublisher;
    private final BookingExpirationService bookingExpirationService;
    private final Clock clock;

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(final CheckoutSessionRequest request) {
        bookingExpirationService.expireExpiredBookings();

        final User currentUser = securityUtils.getCurrentUser();
        final Booking booking = bookingRepository.findByIdWithPaymentDetailsForUpdate(request.bookingId())
                .orElseThrow(() -> new ApiException("Booking hold not found.", HttpStatus.NOT_FOUND));

        validateBookingCanBePaid(booking, currentUser);

        try {
            final Session session = Session.create(
                    buildSessionCreateParams(booking),
                    RequestOptions.builder()
                            .setApiKey(stripeSecretKey())
                            .build()
            );
            extendHoldPaymentWindow(booking);

            final Payment payment = new Payment(
                    booking,
                    session.getId(),
                    booking.getTotalPrice(),
                    stripeCurrency(),
                    OffsetDateTime.now(clock)
            );
            paymentRepository.save(payment);

            return new CheckoutSessionResponse(session.getUrl());
        } catch (StripeException exception) {
            throw new ApiException("Stripe checkout session could not be created.", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    @Transactional
    public void handleStripeWebhook(final String payload, final String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ApiException("Stripe webhook signature is missing.", HttpStatus.BAD_REQUEST);
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret());
        } catch (SignatureVerificationException | IllegalArgumentException exception) {
            log.warn("Stripe webhook signature or payload validation failed: {}", exception.getMessage());
            throw new ApiException("Invalid Stripe webhook payload or signature.", HttpStatus.BAD_REQUEST);
        }

        log.info("Received Stripe webhook event: id={}, type={}", event.getId(), event.getType());

        if (!CHECKOUT_SESSION_COMPLETED.equals(event.getType())) {
            return;
        }

        final StripeObject stripeObject = deserializeStripeObject(event);

        if (stripeObject instanceof Session session) {
            completeCheckoutSession(session);
        }
    }

    private StripeObject deserializeStripeObject(final Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .orElseGet(() -> deserializeStripeObjectUnsafe(event));
    }

    private StripeObject deserializeStripeObjectUnsafe(final Event event) {
        try {
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException exception) {
            log.warn(
                    "Stripe event payload could not be deserialized: eventId={}, type={}, message={}",
                    event.getId(),
                    event.getType(),
                    exception.getMessage()
            );
            throw new ApiException("Stripe event payload could not be deserialized.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateBookingCanBePaid(final Booking booking, final User currentUser) {
        if (!booking.belongsTo(currentUser)) {
            throw new ApiException("Booking hold not found.", HttpStatus.NOT_FOUND);
        }

        if (!canBePaid(booking)) {
            throw new ApiException("Only active booking holds or reservations can be paid.", HttpStatus.BAD_REQUEST);
        }

        if (booking.isExpiredAt(OffsetDateTime.now(clock))) {
            booking.expire();
            projectionSeatEventPublisher.publishSeatMapChanged(booking.getProjection().getId());
            throw new ApiException("Booking has expired.", HttpStatus.BAD_REQUEST);
        }

        if (BookingSeatUtils.activeSeats(booking).isEmpty()) {
            throw new ApiException("Select at least one seat before checkout.", HttpStatus.BAD_REQUEST);
        }
    }

    private SessionCreateParams buildSessionCreateParams(final Booking booking) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(booking.getId().toString())
                .setCustomerEmail(booking.getUser().getEmail())
                .setSuccessUrl(frontendUrlService.checkoutSuccessUrl(booking.getTicketCode()))
                .setCancelUrl(cancelUrl(booking))
                .putMetadata(BOOKING_ID_METADATA_KEY, booking.getId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(stripeCurrency())
                                .setUnitAmount(toMinorUnits(booking.getTotalPrice()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Cinebh tickets - " + booking.getProjection().getMovie().getTitle())
                                        .setDescription(checkoutDescription(booking))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private void completeCheckoutSession(final Session session) {
        paymentRepository.findByStripeSessionIdWithBooking(session.getId())
                .ifPresentOrElse(
                        payment -> completePayment(payment, session),
                        () -> log.warn("Payment row not found for Stripe checkout session: sessionId={}", session.getId())
                );
    }

    private void completePayment(final Payment payment, final Session session) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Stripe payment already completed: paymentId={}", payment.getId());
            return;
        }

        final Booking booking = payment.getBooking();
        final OffsetDateTime now = OffsetDateTime.now(clock);

        if (booking.getStatus() == BookingStatus.PAID) {
            payment.markSucceeded(now);
            log.info("Booking was already paid, marking payment as succeeded: bookingId={}", booking.getId());
            return;
        }

        if (!isPaidCheckoutSession(session)) {
            payment.markFailed();
            log.warn(
                    "Stripe checkout session is not paid: paymentId={}, sessionId={}, stripePaymentStatus={}",
                    payment.getId(),
                    session.getId(),
                    session.getPaymentStatus()
            );
            return;
        }

        if (!isExpectedCheckoutSession(payment, session)) {
            payment.markFailed();
            log.warn(
                    "Stripe checkout session data does not match local payment: paymentId={}, sessionId={}",
                    payment.getId(),
                    session.getId()
            );
            return;
        }

        if (!canBePaid(booking)) {
            payment.markFailed();
            log.warn(
                    "Booking is not payable during Stripe completion: bookingId={}, bookingStatus={}",
                    booking.getId(),
                    booking.getStatus()
            );
            return;
        }

        if (booking.isExpiredAt(now)) {
            booking.expire();
            payment.markFailed();
            projectionSeatEventPublisher.publishSeatMapChanged(booking.getProjection().getId());
            log.warn("Booking expired before Stripe completion: bookingId={}", booking.getId());
            return;
        }

        booking.markPaid();
        payment.markSucceeded(now);
        log.info("Stripe payment completed: paymentId={}, bookingId={}", payment.getId(), booking.getId());

        notificationService.sendTicketPurchaseConfirmation(
                booking.getUser().getEmail(),
                fullName(booking.getUser()),
                booking.getId(),
                booking.getTicketCode(),
                booking.getProjection().getMovie().getTitle(),
                booking.getProjection().getHall().getVenue().getCity().getName(),
                booking.getProjection().getHall().getVenue().getName(),
                booking.getProjection().getHall().getName(),
                booking.getProjection().getStartTime(),
                BookingSeatUtils.activeSeatLabels(booking),
                booking.getTotalPrice(),
                stripeCurrency().toUpperCase(Locale.ROOT)
        );
        projectionSeatEventPublisher.publishSeatMapChanged(booking.getProjection().getId());
    }

    private String checkoutDescription(final Booking booking) {
        return booking.getProjection().getHall().getVenue().getName()
                + ", "
                + booking.getProjection().getHall().getVenue().getCity().getName()
                + " - "
                + booking.getProjection().getStartTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                + " - Seats: "
                + String.join(", ", BookingSeatUtils.activeSeatLabels(booking));
    }

    private long toMinorUnits(final BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    private boolean isPaidCheckoutSession(final Session session) {
        return PAID_PAYMENT_STATUS.equals(session.getPaymentStatus());
    }

    private boolean canBePaid(final Booking booking) {
        return booking.getStatus() == BookingStatus.HOLD
                || booking.getStatus() == BookingStatus.RESERVED;
    }

    private void extendHoldPaymentWindow(final Booking booking) {
        if (booking.getStatus() == BookingStatus.HOLD) {
            booking.extendExpiration(OffsetDateTime.now(clock).plus(BookingSessionDurations.PAYMENT));
        }
    }

    private String cancelUrl(final Booking booking) {
        if (booking.getStatus() == BookingStatus.RESERVED) {
            return frontendUrlService.reservationCheckoutCancelUrl();
        }

        return frontendUrlService.checkoutCancelUrl(
                booking.getProjection().getMovie().getId(),
                booking.getProjection().getId()
        );
    }

    private boolean isExpectedCheckoutSession(final Payment payment, final Session session) {
        final String bookingId = payment.getBooking().getId().toString();
        final String metadataBookingId = session.getMetadata() == null
                ? null
                : session.getMetadata().get(BOOKING_ID_METADATA_KEY);

        return Objects.equals(bookingId, session.getClientReferenceId())
                && Objects.equals(bookingId, metadataBookingId)
                && Objects.equals(toMinorUnits(payment.getAmount()), session.getAmountTotal())
                && payment.getCurrency().equalsIgnoreCase(session.getCurrency());
    }

    private String stripeSecretKey() {
        final String secretKey = stripeProperties().secretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new ApiException("Stripe secret key is not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return secretKey;
    }

    private String stripeWebhookSecret() {
        final String webhookSecret = stripeProperties().webhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ApiException("Stripe webhook secret is not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return webhookSecret;
    }

    private String stripeCurrency() {
        final String currency = stripeProperties().currency();
        return currency == null || currency.isBlank()
                ? "bam"
                : currency.toLowerCase(Locale.ROOT);
    }

    private PaymentProperties.Stripe stripeProperties() {
        final PaymentProperties.Stripe stripe = paymentProperties.stripe();
        if (stripe == null) {
            throw new ApiException("Stripe payment configuration is missing.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return stripe;
    }

    private String fullName(final User user) {
        final String firstName = user.getFirstName();
        final String lastName = user.getLastName();
        final String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
