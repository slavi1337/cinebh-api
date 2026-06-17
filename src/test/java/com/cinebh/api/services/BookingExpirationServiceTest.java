package com.cinebh.api.services;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.Projection;
import com.cinebh.api.entities.SeatTemplate;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.entities.enums.SeatType;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.services.impl.BookingExpirationService;
import com.cinebh.api.websocket.ProjectionSeatEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingExpirationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-25T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ProjectionSeatEventPublisher projectionSeatEventPublisher;

    @Test
    void shouldExpireHoldsAndDeactivateSeats() {
        final Booking booking = new Booking(
                createUser(),
                createProjection(),
                OffsetDateTime.now(FIXED_CLOCK).minusSeconds(1),
                OffsetDateTime.now(FIXED_CLOCK).minusMinutes(5)
        );
        final SeatTemplate seatTemplate = createSeatTemplate();
        booking.replaceActiveSeats(
                List.of(seatTemplate),
                Map.of(SeatType.REGULAR, BigDecimal.valueOf(7))
        );
        final BookingExpirationService expirationService = new BookingExpirationService(
                bookingRepository,
                projectionSeatEventPublisher,
                FIXED_CLOCK
        );

        when(bookingRepository.findExpiredByStatusesForUpdate(
                eq(EnumSet.of(BookingStatus.HOLD, BookingStatus.RESERVED)),
                eq(OffsetDateTime.now(FIXED_CLOCK))
        )).thenReturn(List.of(booking));

        expirationService.expireExpiredBookings();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(booking.getSeats()).allMatch(bookingSeat -> !bookingSeat.isActive());
    }

    private User createUser() {
        final User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.fromString("00000000-0000-0000-0000-000000000111"));
        return user;
    }

    private Projection createProjection() {
        final Projection projection = new Projection();
        ReflectionTestUtils.setField(projection, "id", UUID.fromString("00000000-0000-0000-0000-000000000222"));
        return projection;
    }

    private SeatTemplate createSeatTemplate() {
        final SeatTemplate seatTemplate = new SeatTemplate();
        ReflectionTestUtils.setField(seatTemplate, "id", UUID.fromString("00000000-0000-0000-0000-000000000333"));
        ReflectionTestUtils.setField(seatTemplate, "rowNum", "A");
        ReflectionTestUtils.setField(seatTemplate, "seatNum", "1");
        ReflectionTestUtils.setField(seatTemplate, "type", SeatType.REGULAR);
        return seatTemplate;
    }
}
