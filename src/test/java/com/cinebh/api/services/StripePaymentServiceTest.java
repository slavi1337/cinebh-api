package com.cinebh.api.services;

import com.cinebh.api.config.FrontendProperties;
import com.cinebh.api.config.PaymentProperties;
import com.cinebh.api.dto.payment.CheckoutSessionRequest;
import com.cinebh.api.dto.payment.CheckoutSessionResponse;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.City;
import com.cinebh.api.entities.Hall;
import com.cinebh.api.entities.Movie;
import com.cinebh.api.entities.Payment;
import com.cinebh.api.entities.Projection;
import com.cinebh.api.entities.SeatTemplate;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.Venue;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.entities.enums.PaymentStatus;
import com.cinebh.api.entities.enums.SeatType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.PaymentRepository;
import com.cinebh.api.services.impl.FrontendUrlService;
import com.cinebh.api.services.impl.StripePaymentService;
import com.cinebh.api.utils.SecurityUtils;
import com.cinebh.api.websocket.ProjectionSeatEventPublisher;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID PROJECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000222");
    private static final UUID MOVIE_ID = UUID.fromString("00000000-0000-0000-0000-000000000333");
    private static final UUID SEAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000444");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ProjectionSeatEventPublisher projectionSeatEventPublisher;

    private StripePaymentService paymentService;
    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        paymentService = createPaymentService(
                new PaymentProperties(new PaymentProperties.Stripe("sk_test_secret", "whsec_test", "bam"))
        );

        user = createUser(USER_ID);
        booking = createBooking(user, OffsetDateTime.now(FIXED_CLOCK).plusMinutes(1));
    }

    @Test
    void shouldCreateCheckoutSessionForActiveHold() throws Exception {
        final Session stripeSession = new Session();
        stripeSession.setId("cs_test_123");
        stripeSession.setUrl("https://checkout.stripe.com/c/pay/cs_test_123");

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock
                    .when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(stripeSession);

            final CheckoutSessionResponse response = paymentService.createCheckoutSession(
                    new CheckoutSessionRequest(booking.getId())
            );

            final ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());

            assertThat(response.sessionUrl()).isEqualTo(stripeSession.getUrl());
            assertThat(booking.getExpiresAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK).plusMinutes(5));
            assertThat(paymentCaptor.getValue().getBooking()).isEqualTo(booking);
            assertThat(paymentCaptor.getValue().getStripeSessionId()).isEqualTo(stripeSession.getId());
            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("7.00");
            assertThat(paymentCaptor.getValue().getCurrency()).isEqualTo("bam");
        }
    }

    @Test
    void shouldCreateCheckoutSessionForActiveReservationWithoutExtendingExpiry() throws Exception {
        final OffsetDateTime reservationExpiry = booking.getProjection().getStartTime().minusHours(1);
        booking.markReserved(reservationExpiry);
        final Session stripeSession = new Session();
        stripeSession.setId("cs_test_123");
        stripeSession.setUrl("https://checkout.stripe.com/c/pay/cs_test_123");

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock
                    .when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(stripeSession);

            final CheckoutSessionResponse response = paymentService.createCheckoutSession(
                    new CheckoutSessionRequest(booking.getId())
            );

            final ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());

            assertThat(response.sessionUrl()).isEqualTo(stripeSession.getUrl());
            assertThat(booking.getExpiresAt()).isEqualTo(reservationExpiry);
            assertThat(paymentCaptor.getValue().getBooking()).isEqualTo(booking);
            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Test
    void shouldRejectExpiredHoldBeforeCheckoutSessionIsCreated() {
        final Booking expiredBooking = createBooking(user, OffsetDateTime.now(FIXED_CLOCK).minusSeconds(1));

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(expiredBooking.getId()))
                .thenReturn(Optional.of(expiredBooking));

        assertThatThrownBy(() -> paymentService.createCheckoutSession(new CheckoutSessionRequest(expiredBooking.getId())))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getMessage()).isEqualTo("Booking has expired.");
                });

        assertThat(expiredBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(projectionSeatEventPublisher).publishSeatMapChanged(PROJECTION_ID);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldHideAnotherUsersBookingHold() {
        final User anotherUser = createUser(UUID.fromString("00000000-0000-0000-0000-000000000999"));

        when(securityUtils.getCurrentUser()).thenReturn(anotherUser);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createCheckoutSession(new CheckoutSessionRequest(booking.getId())))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getMessage()).isEqualTo("Booking hold not found.");
                });

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldRejectMissingBookingHoldBeforeCheckoutSessionIsCreated() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(booking.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createCheckoutSession(new CheckoutSessionRequest(booking.getId())))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getMessage()).isEqualTo("Booking hold not found.");
                });

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldRejectCheckoutForNonPayableBookingStatus() {
        booking.markPaid();

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createCheckoutSession(new CheckoutSessionRequest(booking.getId())))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getMessage())
                            .isEqualTo("Only active booking holds or reservations can be paid.");
                });

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldRejectCheckoutWhenBookingHasNoActiveSeats() {
        final Booking bookingWithoutSeats = new Booking(
                user,
                createProjection(),
                OffsetDateTime.now(FIXED_CLOCK).plusMinutes(1),
                OffsetDateTime.now(FIXED_CLOCK)
        );

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(bookingWithoutSeats.getId()))
                .thenReturn(Optional.of(bookingWithoutSeats));

        assertThatThrownBy(() -> paymentService.createCheckoutSession(
                new CheckoutSessionRequest(bookingWithoutSeats.getId())
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getMessage()).isEqualTo("Select at least one seat before checkout.");
                });

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldUseDefaultCurrencyWhenStripeCurrencyIsMissing() throws Exception {
        paymentService = createPaymentService(
                new PaymentProperties(new PaymentProperties.Stripe("sk_test_secret", "whsec_test", null))
        );
        final Session stripeSession = new Session();
        stripeSession.setId("cs_test_123");
        stripeSession.setUrl("https://checkout.stripe.com/c/pay/cs_test_123");

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithPaymentDetailsForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock
                    .when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(stripeSession);

            paymentService.createCheckoutSession(new CheckoutSessionRequest(booking.getId()));

            final ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getCurrency()).isEqualTo("bam");
        }
    }

    @Test
    void shouldRejectWebhookWhenSignatureIsMissing() {
        assertThatThrownBy(() -> paymentService.handleStripeWebhook("{}", " "))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getMessage()).isEqualTo("Stripe webhook signature is missing.");
                });
    }

    @Test
    void shouldIgnoreWebhookEventsThatAreNotCheckoutSessionCompleted() throws Exception {
        final String payload = "{}";
        final String signature = "stripe-signature";
        final Event event = new Event();
        event.setId("evt_charge_succeeded");
        event.setType("charge.succeeded");

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock
                    .when(() -> Webhook.constructEvent(payload, signature, "whsec_test"))
                    .thenReturn(event);

            paymentService.handleStripeWebhook(payload, signature);

            verify(paymentRepository, never()).findByStripeSessionIdWithBooking(any());
        }
    }

    @Test
    void shouldMarkPaymentAsFailedWhenStripeCheckoutSessionIsNotPaid() throws Exception {
        final Payment payment = new Payment(
                booking,
                "cs_test_unpaid",
                booking.getTotalPrice(),
                "bam",
                OffsetDateTime.now(FIXED_CLOCK)
        );
        final Session session = new Session();
        session.setId(payment.getStripeSessionId());
        session.setPaymentStatus("unpaid");

        try (MockedStatic<Webhook> webhookMock = mockCheckoutSessionCompletedWebhook(session)) {
            when(paymentRepository.findByStripeSessionIdWithBooking(payment.getStripeSessionId()))
                    .thenReturn(Optional.of(payment));

            paymentService.handleStripeWebhook("{}", "stripe-signature");
        }

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.HOLD);
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldCompletePaidCheckoutSession() throws Exception {
        final Payment payment = new Payment(
                booking,
                "cs_test_paid",
                booking.getTotalPrice(),
                "bam",
                OffsetDateTime.now(FIXED_CLOCK)
        );
        final Session session = new Session();
        session.setId(payment.getStripeSessionId());
        session.setPaymentStatus("paid");
        session.setClientReferenceId(booking.getId().toString());
        session.setAmountTotal(700L);
        session.setCurrency("bam");
        session.setMetadata(Map.of("bookingId", booking.getId().toString()));

        try (MockedStatic<Webhook> webhookMock = mockCheckoutSessionCompletedWebhook(session)) {
            when(paymentRepository.findByStripeSessionIdWithBooking(payment.getStripeSessionId()))
                    .thenReturn(Optional.of(payment));

            paymentService.handleStripeWebhook("{}", "stripe-signature");
        }

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
        verify(notificationService).sendTicketPurchaseConfirmation(
                eq(user.getEmail()),
                eq("John Doe"),
                eq(booking.getId()),
                eq(booking.getTicketCode()),
                eq("Mandalorian"),
                eq("Banja Luka"),
                eq("Cinebh Arena"),
                eq("Hall 1"),
                eq(booking.getProjection().getStartTime()),
                eq(List.of("A1")),
                eq(BigDecimal.valueOf(7)),
                eq("BAM")
        );
        verify(projectionSeatEventPublisher).publishSeatMapChanged(PROJECTION_ID);
    }

    private StripePaymentService createPaymentService(final PaymentProperties paymentProperties) {
        return new StripePaymentService(
                bookingRepository,
                paymentRepository,
                paymentProperties,
                new FrontendUrlService(new FrontendProperties("https://cinebh.test")),
                securityUtils,
                notificationService,
                projectionSeatEventPublisher,
                FIXED_CLOCK
        );
    }

    private MockedStatic<Webhook> mockCheckoutSessionCompletedWebhook(final Session session) throws Exception {
        final String payload = "{}";
        final String signature = "stripe-signature";
        final Event event = mock(Event.class);
        final EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getId()).thenReturn("evt_checkout_completed");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        final MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class);
        webhookMock
                .when(() -> Webhook.constructEvent(payload, signature, "whsec_test"))
                .thenReturn(event);
        return webhookMock;
    }

    private Booking createBooking(final User bookingUser, final OffsetDateTime expiresAt) {
        final Projection projection = createProjection();
        final SeatTemplate seatTemplate = createSeatTemplate();
        final Booking testBooking = new Booking(
                bookingUser,
                projection,
                expiresAt,
                OffsetDateTime.now(FIXED_CLOCK)
        );
        testBooking.replaceActiveSeats(
                List.of(seatTemplate),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7))
        );
        return testBooking;
    }

    private Projection createProjection() {
        final Projection projection = new Projection();
        ReflectionTestUtils.setField(projection, "id", PROJECTION_ID);
        ReflectionTestUtils.setField(projection, "movie", createMovie());
        ReflectionTestUtils.setField(projection, "hall", createHall());
        ReflectionTestUtils.setField(projection, "startTime", OffsetDateTime.now(FIXED_CLOCK).plusDays(1));
        return projection;
    }

    private Movie createMovie() {
        final Movie movie = new Movie();
        ReflectionTestUtils.setField(movie, "id", MOVIE_ID);
        ReflectionTestUtils.setField(movie, "title", "Mandalorian");
        return movie;
    }

    private Hall createHall() {
        final Hall hall = new Hall();
        ReflectionTestUtils.setField(hall, "name", "Hall 1");
        ReflectionTestUtils.setField(hall, "venue", createVenue());
        return hall;
    }

    private Venue createVenue() {
        final Venue venue = new Venue();
        ReflectionTestUtils.setField(venue, "name", "Cinebh Arena");
        ReflectionTestUtils.setField(venue, "city", createCity());
        return venue;
    }

    private City createCity() {
        final City city = new City();
        ReflectionTestUtils.setField(city, "name", "Banja Luka");
        return city;
    }

    private SeatTemplate createSeatTemplate() {
        final SeatTemplate seatTemplate = new SeatTemplate();
        ReflectionTestUtils.setField(seatTemplate, "id", SEAT_ID);
        ReflectionTestUtils.setField(seatTemplate, "rowNum", "A");
        ReflectionTestUtils.setField(seatTemplate, "seatNum", "1");
        ReflectionTestUtils.setField(seatTemplate, "type", SeatType.REGULAR);
        return seatTemplate;
    }

    private User createUser(final UUID id) {
        final User testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", id);
        ReflectionTestUtils.setField(testUser, "email", "customer@cinebh.test");
        ReflectionTestUtils.setField(testUser, "firstName", "John");
        ReflectionTestUtils.setField(testUser, "lastName", "Doe");
        return testUser;
    }
}
