package com.cinebh.api.mappers;

import com.cinebh.api.dto.booking.ReservationResponse;
import com.cinebh.api.dto.booking.SelectedSeatResponse;
import com.cinebh.api.dto.profile.UserProjectionResponse;
import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.BookingSeat;
import com.cinebh.api.utils.BookingSeatUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingResponseMapper {

    public ReservationResponse toReservationResponse(final Booking booking, final String posterImageUrl) {
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
                toSelectedSeatResponses(booking)
        );
    }

    public UserProjectionResponse toUserProjectionResponse(final Booking booking, final String posterImageUrl) {
        return new UserProjectionResponse(
                booking.getId(),
                booking.getTicketCode(),
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
                booking.getTotalPrice(),
                toSelectedSeatResponses(booking)
        );
    }

    public List<SelectedSeatResponse> toSelectedSeatResponses(final Booking booking) {
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
}
