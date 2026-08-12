package com.cinebh.api.services;

import com.cinebh.api.dto.booking.BookingHoldRequest;
import com.cinebh.api.dto.booking.BookingHoldResponse;
import com.cinebh.api.dto.booking.ReservationResponse;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.City;
import com.cinebh.api.entities.Hall;
import com.cinebh.api.entities.Movie;
import com.cinebh.api.entities.Projection;
import com.cinebh.api.entities.SeatPrice;
import com.cinebh.api.entities.SeatTemplate;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.Venue;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.entities.enums.SeatType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.mappers.BookingResponseMapper;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.BookingSeatRepository;
import com.cinebh.api.repositories.ProjectionRepository;
import com.cinebh.api.repositories.SeatPriceRepository;
import com.cinebh.api.repositories.SeatTemplateRepository;
import com.cinebh.api.services.impl.BookingCoverImageResolver;
import com.cinebh.api.services.impl.BookingServiceImpl;
import com.cinebh.api.utils.SecurityUtils;
import com.cinebh.api.websocket.ProjectionSeatEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID PROJECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000222");
    private static final UUID REGULAR_SEAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000333");
    private static final UUID VIP_SEAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000444");
    private static final UUID MOVIE_ID = UUID.fromString("00000000-0000-0000-0000-000000000555");
    private static final String POSTER_IMAGE_URL = "https://example.com/mandalorian.jpg";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-25T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private ProjectionRepository projectionRepository;
    @Mock
    private SeatTemplateRepository seatTemplateRepository;
    @Mock
    private SeatPriceRepository seatPriceRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ProjectionSeatEventPublisher projectionSeatEventPublisher;
    @Mock
    private NotificationService notificationService;

    private BookingServiceImpl bookingService;
    private User user;
    private Projection projection;
    private SeatTemplate regularSeat;
    private SeatTemplate vipSeat;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                projectionRepository,
                seatTemplateRepository,
                seatPriceRepository,
                bookingRepository,
                bookingSeatRepository,
                securityUtils,
                projectionSeatEventPublisher,
                notificationService,
                new BookingResponseMapper(),
                new BookingCoverImageResolver(bookingRepository),
                FIXED_CLOCK
        );

        user = createUser(USER_ID);
        projection = createProjection(PROJECTION_ID);
        regularSeat = createSeatTemplate(REGULAR_SEAT_ID, "A", "1", SeatType.REGULAR);
        vipSeat = createSeatTemplate(VIP_SEAT_ID, "G", "1", SeatType.VIP);
    }

    @Test
    void shouldReturnSeatMapWithMovieMetadata() {
        mockSeatMapDependencies();
        when(bookingRepository.findCoverImageUrlsByMovieIds(List.of(MOVIE_ID)))
                .thenReturn(Map.of(MOVIE_ID, POSTER_IMAGE_URL));

        final var response = bookingService.getSeatMap(PROJECTION_ID);

        assertThat(response.projectionId()).isEqualTo(PROJECTION_ID);
        assertThat(response.movieId()).isEqualTo(MOVIE_ID);
        assertThat(response.movieTitle()).isEqualTo("Mandalorian");
        assertThat(response.posterImageUrl()).isEqualTo(POSTER_IMAGE_URL);
        assertThat(response.pgRating()).isEqualTo("PG-13");
        assertThat(response.language()).isEqualTo("English");
        assertThat(response.durationMinutes()).isEqualTo(117);
        assertThat(response.seats()).hasSize(2);
    }

    @Test
    void shouldCreateHoldWithFiveMinuteExpiry() {
        mockCommonDependencies();
        when(seatTemplateRepository.findAllById(List.of(REGULAR_SEAT_ID)))
                .thenReturn(List.of(regularSeat));
        when(bookingRepository.findLatestByUserProjectionAndStatusForUpdate(
                USER_ID,
                PROJECTION_ID,
                BookingStatus.HOLD
        )).thenReturn(Optional.empty());
        when(bookingSeatRepository.findUnavailableSeatTemplateIds(
                eq(PROJECTION_ID),
                eq(List.of(REGULAR_SEAT_ID)),
                any(UUID.class)
        )).thenReturn(List.of());
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final BookingHoldResponse response = bookingService.holdSeats(
                new BookingHoldRequest(PROJECTION_ID, List.of(REGULAR_SEAT_ID))
        );

        assertThat(response.projectionId()).isEqualTo(PROJECTION_ID);
        assertThat(response.expiresAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK).plusMinutes(5));
        assertThat(response.totalPrice()).isEqualByComparingTo("7.00");
        assertThat(response.seats()).hasSize(1);
        assertThat(response.seats().getFirst().id()).isEqualTo(REGULAR_SEAT_ID);
    }

    @Test
    void shouldKeepOriginalExpiryWhenExistingHoldChangesSeats() {
        final OffsetDateTime originalExpiry = OffsetDateTime.now(FIXED_CLOCK).plusMinutes(3);
        final Booking existingHold = new Booking(
                user,
                projection,
                originalExpiry,
                OffsetDateTime.now(FIXED_CLOCK)
        );
        existingHold.replaceActiveSeats(
                List.of(regularSeat),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7), SeatType.VIP, BigDecimal.valueOf(10))
        );

        mockCommonDependencies();
        when(seatTemplateRepository.findAllById(List.of(VIP_SEAT_ID)))
                .thenReturn(List.of(vipSeat));
        when(bookingRepository.findLatestByUserProjectionAndStatusForUpdate(
                USER_ID,
                PROJECTION_ID,
                BookingStatus.HOLD
        )).thenReturn(Optional.of(existingHold));
        when(bookingSeatRepository.findUnavailableSeatTemplateIds(
                PROJECTION_ID,
                List.of(VIP_SEAT_ID),
                existingHold.getId()
        )).thenReturn(List.of());
        when(bookingRepository.saveAndFlush(existingHold)).thenReturn(existingHold);

        final BookingHoldResponse response = bookingService.holdSeats(
                new BookingHoldRequest(PROJECTION_ID, List.of(VIP_SEAT_ID))
        );

        assertThat(response.expiresAt()).isEqualTo(originalExpiry);
        assertThat(response.totalPrice()).isEqualByComparingTo("10.00");
        assertThat(response.seats()).hasSize(1);
        assertThat(response.seats().getFirst().id()).isEqualTo(VIP_SEAT_ID);
    }

    @Test
    void shouldThrowConflictWhenSelectedSeatIsUnavailable() {
        mockCommonDependencies();
        when(seatTemplateRepository.findAllById(List.of(REGULAR_SEAT_ID)))
                .thenReturn(List.of(regularSeat));
        when(bookingRepository.findLatestByUserProjectionAndStatusForUpdate(
                USER_ID,
                PROJECTION_ID,
                BookingStatus.HOLD
        )).thenReturn(Optional.empty());
        when(bookingSeatRepository.findUnavailableSeatTemplateIds(
                eq(PROJECTION_ID),
                eq(List.of(REGULAR_SEAT_ID)),
                any(UUID.class)
        )).thenReturn(List.of(REGULAR_SEAT_ID));

        assertThatThrownBy(() -> bookingService.holdSeats(
                new BookingHoldRequest(PROJECTION_ID, List.of(REGULAR_SEAT_ID))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getMessage()).isEqualTo("Some selected seats are no longer available.");
                });

        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void shouldReserveActiveHold() {
        final OffsetDateTime originalExpiry = OffsetDateTime.now(FIXED_CLOCK).plusMinutes(5);
        final Booking existingHold = new Booking(
                user,
                projection,
                originalExpiry,
                OffsetDateTime.now(FIXED_CLOCK)
        );
        existingHold.replaceActiveSeats(
                List.of(regularSeat),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7))
        );

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithDetailsForUpdate(existingHold.getId()))
                .thenReturn(Optional.of(existingHold));
        when(bookingRepository.saveAndFlush(existingHold)).thenReturn(existingHold);
        when(bookingRepository.findCoverImageUrlsByMovieIds(List.of(MOVIE_ID)))
                .thenReturn(Map.of(MOVIE_ID, POSTER_IMAGE_URL));

        final ReservationResponse response = bookingService.reserveHold(existingHold.getId());

        assertThat(existingHold.getStatus()).isEqualTo(BookingStatus.RESERVED);
        assertThat(existingHold.getExpiresAt()).isEqualTo(projection.getStartTime().minusHours(1));
        assertThat(response.bookingId()).isEqualTo(existingHold.getId());
        assertThat(response.movieTitle()).isEqualTo("Mandalorian");
        assertThat(response.posterImageUrl()).isEqualTo(POSTER_IMAGE_URL);
        assertThat(response.pgRating()).isEqualTo("PG-13");
        assertThat(response.language()).isEqualTo("English");
        assertThat(response.durationMinutes()).isEqualTo(117);
        assertThat(response.seats()).hasSize(1);
        verify(projectionSeatEventPublisher).publishSeatMapChanged(PROJECTION_ID);
        verify(notificationService).sendTicketReservationConfirmation(
                eq(user.getEmail()),
                any(),
                eq(existingHold.getId()),
                eq("Mandalorian"),
                eq("Banja Luka"),
                eq("Cinebh Arena"),
                eq("Hall 1"),
                eq(projection.getStartTime()),
                eq(projection.getStartTime().minusHours(1)),
                eq(List.of("A1")),
                eq(BigDecimal.valueOf(7)),
                eq("BAM")
        );
    }

    @Test
    void shouldRejectReservationWhenProjectionStartsWithinOneHour() {
        final Projection soonProjection = createProjection(PROJECTION_ID);
        ReflectionTestUtils.setField(
                soonProjection,
                "startTime",
                OffsetDateTime.now(FIXED_CLOCK).plusMinutes(30)
        );
        final Booking existingHold = new Booking(
                user,
                soonProjection,
                OffsetDateTime.now(FIXED_CLOCK).plusMinutes(5),
                OffsetDateTime.now(FIXED_CLOCK)
        );
        existingHold.replaceActiveSeats(
                List.of(regularSeat),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7))
        );

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithDetailsForUpdate(existingHold.getId()))
                .thenReturn(Optional.of(existingHold));

        assertThatThrownBy(() -> bookingService.reserveHold(existingHold.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getMessage())
                            .isEqualTo("Reservations must be created at least one hour before projection start.");
                });

        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void shouldCancelReservation() {
        final Booking reservation = new Booking(
                user,
                projection,
                projection.getStartTime().minusHours(1),
                OffsetDateTime.now(FIXED_CLOCK)
        );
        reservation.replaceActiveSeats(
                List.of(regularSeat),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7))
        );
        reservation.markReserved(projection.getStartTime().minusHours(1));

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findByIdWithDetailsForUpdate(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        bookingService.cancelReservation(reservation.getId());

        assertThat(reservation.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(reservation.getSeats()).allMatch(bookingSeat -> !bookingSeat.isActive());
        verify(projectionSeatEventPublisher).publishSeatMapChanged(PROJECTION_ID);
    }

    @Test
    void shouldReturnCurrentUserReservations() {
        final Booking reservation = new Booking(
                user,
                projection,
                projection.getStartTime().minusHours(1),
                OffsetDateTime.now(FIXED_CLOCK)
        );
        reservation.replaceActiveSeats(
                List.of(regularSeat),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7))
        );
        reservation.markReserved(projection.getStartTime().minusHours(1));

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(bookingRepository.findReservationsByUserId(USER_ID, OffsetDateTime.now(FIXED_CLOCK)))
                .thenReturn(List.of(reservation));
        when(bookingRepository.findCoverImageUrlsByMovieIds(List.of(MOVIE_ID)))
                .thenReturn(Map.of(MOVIE_ID, POSTER_IMAGE_URL));

        final List<ReservationResponse> reservations = bookingService.getReservations();

        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().bookingId()).isEqualTo(reservation.getId());
        assertThat(reservations.getFirst().movieTitle()).isEqualTo("Mandalorian");
        assertThat(reservations.getFirst().posterImageUrl()).isEqualTo(POSTER_IMAGE_URL);
    }

    private void mockCommonDependencies() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(projectionRepository.findByIdWithDetails(PROJECTION_ID)).thenReturn(Optional.of(projection));
        when(seatPriceRepository.findAll()).thenReturn(List.of(
                createSeatPrice(SeatType.REGULAR, BigDecimal.valueOf(7)),
                createSeatPrice(SeatType.VIP, BigDecimal.valueOf(10)),
                createSeatPrice(SeatType.LOVE, BigDecimal.valueOf(24))
        ));
    }

    private void mockSeatMapDependencies() {
        mockCommonDependencies();
        when(seatTemplateRepository.findAll()).thenReturn(List.of(regularSeat, vipSeat));
        when(bookingSeatRepository.findActiveSeatsForProjection(eq(PROJECTION_ID), any()))
                .thenReturn(List.of());
        when(bookingRepository.findLatestByUserProjectionAndStatus(
                USER_ID,
                PROJECTION_ID,
                BookingStatus.HOLD
        )).thenReturn(Optional.empty());
    }

    private User createUser(final UUID id) {
        final User testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", id);
        ReflectionTestUtils.setField(testUser, "email", "customer@cinebh.test");
        ReflectionTestUtils.setField(testUser, "firstName", "John");
        ReflectionTestUtils.setField(testUser, "lastName", "Doe");
        return testUser;
    }

    private Projection createProjection(final UUID id) {
        final Projection testProjection = new Projection();
        ReflectionTestUtils.setField(testProjection, "id", id);
        ReflectionTestUtils.setField(testProjection, "movie", createMovie());
        ReflectionTestUtils.setField(testProjection, "hall", createHall());
        ReflectionTestUtils.setField(
                testProjection,
                "startTime",
                OffsetDateTime.now(FIXED_CLOCK).plusDays(1)
        );
        return testProjection;
    }

    private Movie createMovie() {
        final Movie movie = new Movie();
        ReflectionTestUtils.setField(movie, "id", MOVIE_ID);
        ReflectionTestUtils.setField(movie, "title", "Mandalorian");
        ReflectionTestUtils.setField(movie, "pgRating", "PG-13");
        ReflectionTestUtils.setField(movie, "language", "English");
        ReflectionTestUtils.setField(movie, "durationMinutes", 117);
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

    private SeatTemplate createSeatTemplate(
            final UUID id,
            final String row,
            final String number,
            final SeatType type
    ) {
        final SeatTemplate seatTemplate = new SeatTemplate();
        ReflectionTestUtils.setField(seatTemplate, "id", id);
        ReflectionTestUtils.setField(seatTemplate, "rowNum", row);
        ReflectionTestUtils.setField(seatTemplate, "seatNum", number);
        ReflectionTestUtils.setField(seatTemplate, "type", type);
        return seatTemplate;
    }

    private SeatPrice createSeatPrice(final SeatType seatType, final BigDecimal price) {
        final SeatPrice seatPrice = new SeatPrice();
        ReflectionTestUtils.setField(seatPrice, "seatType", seatType);
        ReflectionTestUtils.setField(seatPrice, "price", price);
        return seatPrice;
    }
}
