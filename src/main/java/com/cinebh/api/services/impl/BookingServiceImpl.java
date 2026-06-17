package com.cinebh.api.services.impl;

import com.cinebh.api.dto.booking.BookingHoldRequest;
import com.cinebh.api.dto.booking.BookingHoldResponse;
import com.cinebh.api.dto.booking.ReservationResponse;
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
import com.cinebh.api.services.NotificationService;
import com.cinebh.api.utils.BookingSessionDurations;
import com.cinebh.api.utils.BookingSeatUtils;
import com.cinebh.api.utils.SecurityUtils;
import com.cinebh.api.websocket.ProjectionSeatEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
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

    private static final String DEFAULT_CURRENCY = "BAM";
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
    private final NotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional
    public SeatMapResponse getSeatMap(final UUID projectionId) {
        bookingExpirationService.expireExpiredBookings();

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
                findCoverImageUrl(projection.getMovie().getId()),
                projection.getMovie().getPgRating(),
                projection.getMovie().getLanguage(),
                projection.getMovie().getDurationMinutes(),
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
        bookingExpirationService.expireExpiredBookings();

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
        bookingExpirationService.expireExpiredBookings();

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

    @Override
    @Transactional
    public ReservationResponse reserveHold(final UUID bookingId) {
        bookingExpirationService.expireExpiredBookings();

        final User currentUser = securityUtils.getCurrentUser();
        final Booking booking = bookingRepository.findByIdWithDetailsForUpdate(bookingId)
                .orElseThrow(() -> new ApiException("Booking hold not found.", HttpStatus.NOT_FOUND));

        validateBookingCanBeReserved(booking, currentUser);

        final OffsetDateTime reservationExpiresAt = reservationExpirationTime(booking);
        booking.markReserved(reservationExpiresAt);

        final Booking savedBooking = bookingRepository.saveAndFlush(booking);
        projectionSeatEventPublisher.publishSeatMapChanged(savedBooking.getProjection().getId());
        sendReservationConfirmation(savedBooking);

        return toReservationResponse(savedBooking, findCoverImageUrl(savedBooking));
    }

    @Override
    @Transactional
    public List<ReservationResponse> getReservations() {
        bookingExpirationService.expireExpiredBookings();

        final User currentUser = securityUtils.getCurrentUser();
        final List<Booking> reservations =
                bookingRepository.findReservationsByUserId(currentUser.getId(), OffsetDateTime.now(clock));
        final Map<UUID, String> coverImageUrlsByMovieId = findCoverImageUrlsByMovieId(reservations);

        return reservations
                .stream()
                .map(booking -> toReservationResponse(
                        booking,
                        coverImageUrlsByMovieId.get(booking.getProjection().getMovie().getId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public void cancelReservation(final UUID bookingId) {
        bookingExpirationService.expireExpiredBookings();

        final User currentUser = securityUtils.getCurrentUser();
        final Booking booking = bookingRepository.findByIdWithDetailsForUpdate(bookingId)
                .orElseThrow(() -> new ApiException("Reservation not found.", HttpStatus.NOT_FOUND));

        if (!booking.belongsTo(currentUser)) {
            throw new ApiException("Reservation not found.", HttpStatus.NOT_FOUND);
        }

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new ApiException("Only active reservations can be cancelled.", HttpStatus.BAD_REQUEST);
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

    private void validateBookingCanBeReserved(final Booking booking, final User currentUser) {
        if (!booking.belongsTo(currentUser)) {
            throw new ApiException("Booking hold not found.", HttpStatus.NOT_FOUND);
        }

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new ApiException("Only active booking holds can be reserved.", HttpStatus.BAD_REQUEST);
        }

        if (booking.isExpiredAt(OffsetDateTime.now(clock))) {
            booking.expire();
            projectionSeatEventPublisher.publishSeatMapChanged(booking.getProjection().getId());
            throw new ApiException("Booking hold has expired.", HttpStatus.BAD_REQUEST);
        }

        if (BookingSeatUtils.activeSeats(booking).isEmpty()) {
            throw new ApiException("Select at least one seat before reservation.", HttpStatus.BAD_REQUEST);
        }

        if (!reservationExpirationTime(booking).isAfter(OffsetDateTime.now(clock))) {
            throw new ApiException(
                    "Reservations must be created at least one hour before projection start.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private OffsetDateTime reservationExpirationTime(final Booking booking) {
        return booking.getProjection().getStartTime().minus(BookingSessionDurations.RESERVATION_CUTOFF);
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
        return new Booking(
                currentUser,
                projection,
                now.plus(BookingSessionDurations.SEAT_SELECTION),
                now
        );
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

        return BookingSeatUtils.sortSeatTemplates(seatTemplates);
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

        return BookingSeatUtils.sortSeatTemplates(seatTemplates);
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
                activeSelectedSeats(booking)
        );
    }

    private ReservationResponse toReservationResponse(final Booking booking, final String posterImageUrl) {
        return new ReservationResponse(
                booking.getId(),
                booking.getProjection().getMovie().getId(),
                booking.getProjection().getId(),
                booking.getProjection().getMovie().getTitle(),
                posterImageUrl,
                booking.getProjection().getMovie().getPgRating(),
                booking.getProjection().getMovie().getLanguage(),
                booking.getProjection().getMovie().getDurationMinutes(),
                booking.getProjection().getHall().getVenue().getCity().getName(),
                booking.getProjection().getHall().getVenue().getName(),
                booking.getProjection().getHall().getName(),
                booking.getProjection().getStartTime(),
                booking.getExpiresAt(),
                booking.getTotalPrice(),
                activeSelectedSeats(booking)
        );
    }

    private String findCoverImageUrl(final Booking booking) {
        return findCoverImageUrl(booking.getProjection().getMovie().getId());
    }

    private String findCoverImageUrl(final UUID movieId) {
        return bookingRepository.findCoverImageUrlsByMovieIds(List.of(movieId)).get(movieId);
    }

    private Map<UUID, String> findCoverImageUrlsByMovieId(final List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Map.of();
        }

        final List<UUID> movieIds = bookings.stream()
                .map(booking -> booking.getProjection().getMovie().getId())
                .distinct()
                .toList();

        return bookingRepository.findCoverImageUrlsByMovieIds(movieIds);
    }

    private List<SelectedSeatResponse> activeSelectedSeats(final Booking booking) {
        return booking.getSeats()
                .stream()
                .filter(BookingSeat::isActive)
                .map(this::toSelectedSeatResponse)
                .sorted(BookingSeatUtils.seatPositionComparator(
                        SelectedSeatResponse::row,
                        SelectedSeatResponse::number
                ))
                .toList();
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

    private void sendReservationConfirmation(final Booking booking) {
        notificationService.sendTicketReservationConfirmation(
                booking.getUser().getEmail(),
                fullName(booking.getUser()),
                booking.getId(),
                booking.getProjection().getMovie().getTitle(),
                booking.getProjection().getHall().getVenue().getCity().getName(),
                booking.getProjection().getHall().getVenue().getName(),
                booking.getProjection().getHall().getName(),
                booking.getProjection().getStartTime(),
                booking.getExpiresAt(),
                BookingSeatUtils.activeSeatLabels(booking),
                booking.getTotalPrice(),
                DEFAULT_CURRENCY
        );
    }

    private String fullName(final User user) {
        final String firstName = user.getFirstName();
        final String lastName = user.getLastName();
        final String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
