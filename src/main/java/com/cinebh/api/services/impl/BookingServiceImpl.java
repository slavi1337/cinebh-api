package com.cinebh.api.services.impl;

import com.cinebh.api.dto.booking.BookingHoldRequest;
import com.cinebh.api.dto.booking.BookingHoldResponse;
import com.cinebh.api.dto.booking.SeatAvailabilityStatus;
import com.cinebh.api.dto.booking.SeatMapResponse;
import com.cinebh.api.dto.booking.SeatResponse;
import com.cinebh.api.dto.booking.SelectedSeatResponse;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.BookingSeat;
import com.cinebh.api.entities.Projection;
import com.cinebh.api.entities.SeatPrice;
import com.cinebh.api.entities.SeatTemplate;
import com.cinebh.api.entities.User;
import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.entities.enums.SeatType;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.BookingRepository;
import com.cinebh.api.repositories.BookingSeatRepository;
import com.cinebh.api.repositories.ProjectionRepository;
import com.cinebh.api.repositories.SeatPriceRepository;
import com.cinebh.api.repositories.SeatTemplateRepository;
import com.cinebh.api.services.BookingService;
import com.cinebh.api.utils.SecurityUtils;
import com.cinebh.api.websocket.ProjectionSeatEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);
    private static final Set<BookingStatus> BLOCKING_STATUSES = EnumSet.of(
            BookingStatus.HOLD,
            BookingStatus.RESERVED,
            BookingStatus.PAID
    );

    private final ProjectionRepository projectionRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final SeatPriceRepository seatPriceRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingExpirationService bookingExpirationService;
    private final SecurityUtils securityUtils;
    private final ProjectionSeatEventPublisher projectionSeatEventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public SeatMapResponse getSeatMap(final UUID projectionId) {
        bookingExpirationService.expireExpiredHolds();

        final User currentUser = securityUtils.getCurrentUser();
        final Projection projection = findProjection(projectionId);
        final Map<SeatType, BigDecimal> seatPrices = loadSeatPrices();
        final List<SeatTemplate> seatTemplates = loadSeatTemplates();
        final Map<UUID, BookingSeat> activeSeatsByTemplateId = bookingSeatRepository
                .findActiveSeatsForProjection(projectionId, BLOCKING_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        bookingSeat -> bookingSeat.getSeatTemplate().getId(),
                        Function.identity()
                ));

        final BookingHoldResponse activeHold = findCurrentUserHold(currentUser, projectionId)
                .map(this::toHoldResponse)
                .orElse(null);

        final List<SeatResponse> seats = seatTemplates.stream()
                .map(seatTemplate -> toSeatResponse(
                        seatTemplate,
                        seatPrices,
                        activeSeatsByTemplateId.get(seatTemplate.getId()),
                        currentUser
                ))
                .toList();

        return new SeatMapResponse(
                projection.getId(),
                projection.getMovie().getId(),
                projection.getMovie().getTitle(),
                projection.getHall().getVenue().getCity().getName(),
                projection.getHall().getVenue().getName(),
                projection.getHall().getName(),
                projection.getStartTime(),
                projection.getEndTime(),
                seats,
                activeHold
        );
    }

    @Override
    @Transactional
    public BookingHoldResponse holdSeats(final BookingHoldRequest request) {
        bookingExpirationService.expireExpiredHolds();

        final User currentUser = securityUtils.getCurrentUser();
        final Projection projection = findProjection(request.projectionId());
        validateProjectionCanBeBooked(projection);

        final List<UUID> requestedSeatIds = normalizeSeatTemplateIds(request.seatTemplateIds());
        final List<SeatTemplate> selectedSeatTemplates = findRequestedSeatTemplates(requestedSeatIds);
        final Map<SeatType, BigDecimal> seatPrices = loadSeatPrices();

        Booking booking = findCurrentUserHold(currentUser, projection.getId())
                .orElseGet(() -> createHold(currentUser, projection));

        if (booking.isExpiredAt(OffsetDateTime.now(clock))) {
            booking.expire();
            bookingRepository.save(booking);
            booking = createHold(currentUser, projection);
        }

        ensureSeatsAvailable(projection.getId(), requestedSeatIds, booking.getId());
        booking.replaceActiveSeats(selectedSeatTemplates, seatPrices);

        try {
            final Booking savedBooking = bookingRepository.saveAndFlush(booking);
            projectionSeatEventPublisher.publishSeatMapChanged(projection.getId());
            return toHoldResponse(savedBooking);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException("Some selected seats are no longer available.", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public void cancelHold(final UUID bookingId) {
        bookingExpirationService.expireExpiredHolds();

        final User currentUser = securityUtils.getCurrentUser();
        final Booking booking = bookingRepository.findByIdWithSeats(bookingId)
                .orElseThrow(() -> new ApiException("Booking hold not found.", HttpStatus.NOT_FOUND));

        if (!booking.belongsTo(currentUser)) {
            throw new ApiException("Booking hold not found.", HttpStatus.NOT_FOUND);
        }

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new ApiException("Only active booking holds can be cancelled.", HttpStatus.BAD_REQUEST);
        }

        booking.cancel();
        projectionSeatEventPublisher.publishSeatMapChanged(booking.getProjection().getId());
    }

    private Projection findProjection(final UUID projectionId) {
        return projectionRepository.findByIdWithDetails(projectionId)
                .orElseThrow(() -> new ApiException("Projection not found.", HttpStatus.NOT_FOUND));
    }

    private void validateProjectionCanBeBooked(final Projection projection) {
        if (!projection.getStartTime().isAfter(OffsetDateTime.now(clock))) {
            throw new ApiException("Projection is no longer available for booking.", HttpStatus.BAD_REQUEST);
        }
    }

    private Optional<Booking> findCurrentUserHold(final User currentUser, final UUID projectionId) {
        return bookingRepository
                .findLatestByUserProjectionAndStatusForUpdate(
                        currentUser.getId(),
                        projectionId,
                        BookingStatus.HOLD
                );
    }

    private Booking createHold(final User currentUser, final Projection projection) {
        final OffsetDateTime now = OffsetDateTime.now(clock);
        return new Booking(currentUser, projection, now.plus(HOLD_DURATION), now);
    }

    private List<UUID> normalizeSeatTemplateIds(final List<UUID> seatTemplateIds) {
        return new LinkedHashSet<>(seatTemplateIds).stream().toList();
    }

    private List<SeatTemplate> findRequestedSeatTemplates(final List<UUID> requestedSeatIds) {
        if (requestedSeatIds.isEmpty()) {
            return List.of();
        }

        final List<SeatTemplate> seatTemplates = seatTemplateRepository.findAllById(requestedSeatIds);
        final Set<UUID> foundSeatIds = seatTemplates.stream()
                .map(SeatTemplate::getId)
                .collect(Collectors.toSet());

        if (!foundSeatIds.containsAll(requestedSeatIds)) {
            throw new ApiException("One or more selected seats do not exist.", HttpStatus.BAD_REQUEST);
        }

        return sortSeatTemplates(seatTemplates);
    }

    private void ensureSeatsAvailable(
            final UUID projectionId,
            final List<UUID> requestedSeatIds,
            final UUID currentBookingId
    ) {
        if (requestedSeatIds.isEmpty()) {
            return;
        }

        final List<UUID> unavailableSeatIds = bookingSeatRepository.findUnavailableSeatTemplateIds(
                projectionId,
                requestedSeatIds,
                currentBookingId
        );

        if (!unavailableSeatIds.isEmpty()) {
            throw new ApiException("Some selected seats are no longer available.", HttpStatus.CONFLICT);
        }
    }

    private Map<SeatType, BigDecimal> loadSeatPrices() {
        final Map<SeatType, BigDecimal> seatPrices = seatPriceRepository.findAll()
                .stream()
                .collect(Collectors.toMap(SeatPrice::getSeatType, SeatPrice::getPrice));

        final Set<SeatType> missingSeatTypes = new HashSet<>(Arrays.asList(SeatType.values()));
        missingSeatTypes.removeAll(seatPrices.keySet());

        if (!missingSeatTypes.isEmpty()) {
            throw new ApiException("Seat prices are not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return seatPrices;
    }

    private List<SeatTemplate> loadSeatTemplates() {
        final List<SeatTemplate> seatTemplates = seatTemplateRepository.findAll();

        if (seatTemplates.isEmpty()) {
            throw new ApiException("Seat layout is not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return sortSeatTemplates(seatTemplates);
    }

    private List<SeatTemplate> sortSeatTemplates(final Collection<SeatTemplate> seatTemplates) {
        return seatTemplates.stream()
                .sorted(Comparator
                        .comparing(SeatTemplate::getRowNum)
                        .thenComparingInt(seatTemplate -> parseSeatNumber(seatTemplate.getSeatNum())))
                .toList();
    }

    private int parseSeatNumber(final String seatNumber) {
        try {
            return Integer.parseInt(seatNumber);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private SeatResponse toSeatResponse(
            final SeatTemplate seatTemplate,
            final Map<SeatType, BigDecimal> seatPrices,
            final BookingSeat activeBookingSeat,
            final User currentUser
    ) {
        final boolean selectedByCurrentUser = activeBookingSeat != null
                && activeBookingSeat.getBooking().getStatus() == BookingStatus.HOLD
                && activeBookingSeat.getBooking().belongsTo(currentUser);

        return new SeatResponse(
                seatTemplate.getId(),
                seatTemplate.getRowNum(),
                seatTemplate.getSeatNum(),
                seatTemplate.getType(),
                seatPrices.get(seatTemplate.getType()),
                toSeatStatus(activeBookingSeat),
                selectedByCurrentUser
        );
    }

    private SeatAvailabilityStatus toSeatStatus(final BookingSeat activeBookingSeat) {
        if (activeBookingSeat == null) {
            return SeatAvailabilityStatus.AVAILABLE;
        }

        return switch (activeBookingSeat.getBooking().getStatus()) {
            case HOLD -> SeatAvailabilityStatus.HELD;
            case RESERVED -> SeatAvailabilityStatus.RESERVED;
            case PAID -> SeatAvailabilityStatus.PAID;
            case CANCELLED, EXPIRED -> SeatAvailabilityStatus.AVAILABLE;
        };
    }

    private BookingHoldResponse toHoldResponse(final Booking booking) {
        return new BookingHoldResponse(
                booking.getId(),
                booking.getProjection().getId(),
                booking.getExpiresAt(),
                booking.getTotalPrice(),
                booking.getSeats()
                        .stream()
                        .filter(BookingSeat::isActive)
                        .map(this::toSelectedSeatResponse)
                        .sorted(Comparator
                                .comparing(SelectedSeatResponse::row)
                                .thenComparingInt(seat -> parseSeatNumber(seat.number())))
                        .toList()
        );
    }

    private SelectedSeatResponse toSelectedSeatResponse(final BookingSeat bookingSeat) {
        return new SelectedSeatResponse(
                bookingSeat.getSeatTemplate().getId(),
                bookingSeat.getSeatTemplate().getRowNum(),
                bookingSeat.getSeatTemplate().getSeatNum(),
                bookingSeat.getSeatTemplate().getType(),
                bookingSeat.getPriceSnapshot()
        );
    }
}
