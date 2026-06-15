package com.cinebh.api.services;

import com.cinebh.api.dto.booking.BookingHoldRequest;
import com.cinebh.api.dto.booking.BookingHoldResponse;
import com.cinebh.api.dto.booking.SeatMapResponse;

import java.util.UUID;

public interface BookingService {

    SeatMapResponse getSeatMap(UUID projectionId);

    BookingHoldResponse holdSeats(BookingHoldRequest request);

    void cancelHold(UUID bookingId);
}
