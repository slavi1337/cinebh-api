package com.cinebh.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_template_id", nullable = false)
    private SeatTemplate seatTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projection_id", nullable = false)
    private Projection projection;

    @Column(name = "price_snapshot", nullable = false)
    private BigDecimal priceSnapshot;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public BookingSeat(
            final Booking booking,
            final SeatTemplate seatTemplate,
            final Projection projection,
            final BigDecimal priceSnapshot
    ) {
        this.id = UUID.randomUUID();
        this.booking = booking;
        this.seatTemplate = seatTemplate;
        this.projection = projection;
        this.priceSnapshot = priceSnapshot;
        this.active = true;
    }

    public void activate(final BigDecimal priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
        this.active = true;
    }

    public void deactivate() {
        active = false;
    }
}
