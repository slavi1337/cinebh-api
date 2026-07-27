package com.cinebh.api.services.impl;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingCoverImageResolver {

    private final BookingRepository bookingRepository;

    public String findCoverImageUrl(final Booking booking) {
        return findCoverImageUrl(booking.getProjection().getMovie().getId());
    }

    public String findCoverImageUrl(final UUID movieId) {
        return bookingRepository.findCoverImageUrlsByMovieIds(List.of(movieId)).get(movieId);
    }

    public Map<UUID, String> findCoverImageUrlsByMovieId(final List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Map.of();
        }

        final List<UUID> movieIds = bookings.stream()
                .map(booking -> booking.getProjection().getMovie().getId())
                .distinct()
                .toList();

        return bookingRepository.findCoverImageUrlsByMovieIds(movieIds);
    }
}
