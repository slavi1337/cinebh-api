package com.cinebh.api.repositories;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.repositories.custom.BookingQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>, BookingQueryRepository {
}
