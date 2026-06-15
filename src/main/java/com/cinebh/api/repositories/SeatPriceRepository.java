package com.cinebh.api.repositories;

import com.cinebh.api.entities.SeatPrice;
import com.cinebh.api.entities.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatPriceRepository extends JpaRepository<SeatPrice, SeatType> {
}
