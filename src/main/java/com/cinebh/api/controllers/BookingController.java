package com.cinebh.api.controllers;

import com.cinebh.api.dto.booking.BookingHoldRequest;
import com.cinebh.api.dto.booking.BookingHoldResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
