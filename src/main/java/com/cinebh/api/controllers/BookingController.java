package com.cinebh.api.controllers;

import com.cinebh.api.dto.booking.BookingHoldRequest;
import com.cinebh.api.dto.booking.BookingHoldResponse;
import com.cinebh.api.dto.booking.ReservationResponse;
import com.cinebh.api.services.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Authenticated booking hold endpoints")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/holds")
    @Operation(
            summary = "Create or update a booking hold",
            description = "Locks selected seats for the current user without resetting the original hold timer"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking hold updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking hold request"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "Projection not found"),
            @ApiResponse(responseCode = "409", description = "One or more selected seats are unavailable"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<BookingHoldResponse> holdSeats(@Valid @RequestBody BookingHoldRequest request) {
        return ResponseEntity.ok(bookingService.holdSeats(request));
    }

    @DeleteMapping("/holds/{bookingId}")
    @Operation(
            summary = "Cancel a booking hold",
            description = "Cancels the current user's active booking hold and releases its selected seats"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booking hold cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Booking is not an active hold"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "Booking hold not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> cancelHold(@PathVariable UUID bookingId) {
        bookingService.cancelHold(bookingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/holds/{bookingId}/reserve")
    @Operation(
            summary = "Reserve a booking hold",
            description = "Converts the current user's active booking hold into a reservation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking reservation created successfully"),
            @ApiResponse(responseCode = "400", description = "Booking hold cannot be reserved"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "Booking hold not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ReservationResponse> reserveHold(@PathVariable final UUID bookingId) {
        return ResponseEntity.ok(bookingService.reserveHold(bookingId));
    }

    @GetMapping("/reservations")
    @Operation(
            summary = "Get current user's reservations",
            description = "Returns active ticket reservations for the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservations returned successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<ReservationResponse>> getReservations() {
        return ResponseEntity.ok(bookingService.getReservations());
    }

    @DeleteMapping("/reservations/{bookingId}")
    @Operation(
            summary = "Cancel reservation",
            description = "Cancels the current user's active reservation and releases selected seats"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reservation cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Booking is not an active reservation"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> cancelReservation(@PathVariable final UUID bookingId) {
        bookingService.cancelReservation(bookingId);
        return ResponseEntity.noContent().build();
    }
}
