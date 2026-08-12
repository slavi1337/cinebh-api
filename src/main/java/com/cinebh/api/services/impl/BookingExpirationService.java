package com.cinebh.api.services.impl;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.websocket.ProjectionSeatEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingExpirationService {

    private static final Set<BookingStatus> EXPIRABLE_STATUSES = EnumSet.of(
            BookingStatus.HOLD,
            BookingStatus.RESERVED
    );

    private final BookingRepository bookingRepository;
    private final ProjectionSeatEventPublisher projectionSeatEventPublisher;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${cinebh.booking.cleanup-delay-ms:${cinebh.booking.hold-cleanup-delay-ms:30000}}")
    @Transactional
    public void expireExpiredBookings() {
        final List<Booking> expiredBookings = bookingRepository.findExpiredByStatusesForUpdate(
                EXPIRABLE_STATUSES,
                OffsetDateTime.now(clock)
        );

        expiredBookings.forEach(booking -> {
            booking.expire();
            projectionSeatEventPublisher.publishSeatMapChanged(booking.getProjection().getId());
        });
    }
}
