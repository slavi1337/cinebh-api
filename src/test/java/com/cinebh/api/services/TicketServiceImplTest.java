package com.cinebh.api.services;

import com.cinebh.api.dto.ticket.TicketValidationStatus;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.City;
import com.cinebh.api.entities.Hall;
import com.cinebh.api.entities.Movie;
import com.cinebh.api.entities.Payment;
import com.cinebh.api.entities.Projection;
import com.cinebh.api.entities.SeatTemplate;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.Venue;
import com.cinebh.api.entities.enums.SeatType;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.PaymentRepository;
import com.cinebh.api.services.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final UUID TICKET_CODE = UUID.fromString("00000000-0000-0000-0000-000000000111");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(bookingRepository, paymentRepository);
    }

    @Test
    void shouldReturnValidTicketDetailsForPaidBooking() {
        final Booking booking = createBooking();
        final Payment payment = new Payment(
                booking,
                "cs_test_123",
                BigDecimal.valueOf(14),
                "bam",
                OffsetDateTime.now()
        );
        booking.markPaid();
        payment.markSucceeded(OffsetDateTime.now());

        when(bookingRepository.findByTicketCodeWithPaymentDetails(TICKET_CODE))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findSuccessfulByBookingId(booking.getId()))
                .thenReturn(Optional.of(payment));

        final var response = ticketService.validateTicket(TICKET_CODE);

        assertThat(response.valid()).isTrue();
        assertThat(response.status()).isEqualTo(TicketValidationStatus.VALID);
        assertThat(response.ticket()).isNotNull();
        assertThat(response.ticket().bookingId()).isEqualTo(booking.getId());
        assertThat(response.ticket().ticketCode()).isEqualTo(TICKET_CODE);
        assertThat(response.ticket().movieTitle()).isEqualTo("Mandalorian");
        assertThat(response.ticket().cityName()).isEqualTo("Banja Luka");
        assertThat(response.ticket().venueName()).isEqualTo("Cinebh Arena");
        assertThat(response.ticket().seats()).containsExactly("A1", "A2");
        assertThat(response.ticket().totalPaid()).isEqualByComparingTo("14");
        assertThat(response.ticket().currency()).isEqualTo("BAM");
    }

    @Test
    void shouldReturnPendingWhenBookingIsNotPaidYet() {
        final Booking booking = createBooking();

        when(bookingRepository.findByTicketCodeWithPaymentDetails(TICKET_CODE))
                .thenReturn(Optional.of(booking));

        final var response = ticketService.validateTicket(TICKET_CODE);

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo(TicketValidationStatus.PENDING);
        assertThat(response.ticket()).isNull();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldReturnInvalidWhenTicketCodeIsUnknown() {
        when(bookingRepository.findByTicketCodeWithPaymentDetails(TICKET_CODE))
                .thenReturn(Optional.empty());

        final var response = ticketService.validateTicket(TICKET_CODE);

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo(TicketValidationStatus.INVALID);
        assertThat(response.ticket()).isNull();
        verifyNoInteractions(paymentRepository);
    }

    private Booking createBooking() {
        final User user = new User();
        final Projection projection = createProjection();
        final Booking booking = new Booking(
                user,
                projection,
                OffsetDateTime.now().plusMinutes(5),
                OffsetDateTime.now()
        );

        ReflectionTestUtils.setField(booking, "ticketCode", TICKET_CODE);
        booking.replaceActiveSeats(
                List.of(
                        createSeatTemplate("A", "2"),
                        createSeatTemplate("A", "1")
                ),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7))
        );

        return booking;
    }

    private Projection createProjection() {
        final Projection projection = new Projection();
        ReflectionTestUtils.setField(projection, "movie", createMovie());
        ReflectionTestUtils.setField(projection, "hall", createHall());
        ReflectionTestUtils.setField(projection, "startTime", OffsetDateTime.parse("2026-06-15T18:00:00Z"));
        return projection;
    }

    private Movie createMovie() {
        final Movie movie = new Movie();
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

    private SeatTemplate createSeatTemplate(final String row, final String number) {
        final SeatTemplate seatTemplate = new SeatTemplate();
        ReflectionTestUtils.setField(seatTemplate, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(seatTemplate, "rowNum", row);
        ReflectionTestUtils.setField(seatTemplate, "seatNum", number);
        ReflectionTestUtils.setField(seatTemplate, "type", SeatType.REGULAR);
        return seatTemplate;
    }
}
