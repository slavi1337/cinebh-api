package com.cinebh.api.entities;

import com.cinebh.api.entities.enums.BookingStatus;
import com.cinebh.api.entities.enums.SeatType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projection_id", nullable = false)
    private Projection projection;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booking_status")
    private BookingStatus status;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "ticket_code", nullable = false, unique = true)
    private UUID ticketCode;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_reminder_enabled", nullable = false)
    private boolean reminderEnabled;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingSeat> seats = new ArrayList<>();

    public Booking(
            final User user,
            final Projection projection,
            final OffsetDateTime expiresAt,
            final OffsetDateTime createdAt
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.projection = projection;
        this.status = BookingStatus.HOLD;
        this.totalPrice = BigDecimal.ZERO;
        this.ticketCode = UUID.randomUUID();
        this.expiresAt = expiresAt;
        this.reminderEnabled = false;
        this.createdAt = createdAt;
    }

    public boolean isExpiredAt(final OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean belongsTo(final User user) {
        return Objects.equals(this.user.getId(), user.getId());
    }

    public void replaceActiveSeats(
            final Collection<SeatTemplate> selectedSeatTemplates,
            final Map<SeatType, BigDecimal> seatPrices
    ) {
        final Set<UUID> selectedSeatTemplateIds = selectedSeatTemplates.stream()
                .map(SeatTemplate::getId)
                .collect(Collectors.toSet());

        seats.stream()
                .filter(BookingSeat::isActive)
                .filter(bookingSeat -> !selectedSeatTemplateIds.contains(bookingSeat.getSeatTemplate().getId()))
                .forEach(BookingSeat::deactivate);

        selectedSeatTemplates.forEach(seatTemplate -> activateSeat(seatTemplate, seatPrices.get(seatTemplate.getType())));
        recalculateTotalPrice();
    }

    public void cancel() {
        status = BookingStatus.CANCELLED;
        deactivateSeats();
        recalculateTotalPrice();
    }

    public void expire() {
        status = BookingStatus.EXPIRED;
        deactivateSeats();
        recalculateTotalPrice();
    }

    public void markPaid() {
        status = BookingStatus.PAID;
    }

    public void extendExpiration(final OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    private void activateSeat(final SeatTemplate seatTemplate, final BigDecimal price) {
        findSeat(seatTemplate.getId())
                .ifPresentOrElse(
                        bookingSeat -> bookingSeat.activate(price),
                        () -> seats.add(new BookingSeat(this, seatTemplate, projection, price))
                );
    }

    private Optional<BookingSeat> findSeat(final UUID seatTemplateId) {
        return seats.stream()
                .filter(bookingSeat -> Objects.equals(bookingSeat.getSeatTemplate().getId(), seatTemplateId))
                .findFirst();
    }

    private void deactivateSeats() {
        seats.stream()
                .filter(BookingSeat::isActive)
                .forEach(BookingSeat::deactivate);
    }

    private void recalculateTotalPrice() {
        totalPrice = seats.stream()
                .filter(BookingSeat::isActive)
                .map(BookingSeat::getPriceSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
