package com.cinebh.api.entities;

import com.cinebh.api.entities.enums.SeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "seat_prices")
public class SeatPrice {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "seat_type", nullable = false, columnDefinition = "seat_type")
    private SeatType seatType;

    @Column(nullable = false)
    private BigDecimal price;
}
