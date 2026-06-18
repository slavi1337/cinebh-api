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

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "seat_templates")
public class SeatTemplate {

    @Id
    private UUID id;

    @Column(name = "row_num", nullable = false)
    private String rowNum;

    @Column(name = "seat_num", nullable = false)
    private String seatNum;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "seat_type")
    private SeatType type;
}
