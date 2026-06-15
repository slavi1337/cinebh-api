package com.cinebh.api.controllers;

import com.cinebh.api.dto.booking.SeatMapResponse;
import com.cinebh.api.services.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projections")
@Tag(name = "Projection Seats", description = "Authenticated seat map endpoints for projection booking")
public class ProjectionSeatController {

    private final BookingService bookingService;

    @GetMapping("/{projectionId}/seat-map")
    @Operation(
            summary = "Get projection seat map",
            description = "Returns seat layout, prices, availability and the current user's active hold"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seat map fetched successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "Projection not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<SeatMapResponse> getSeatMap(@PathVariable UUID projectionId) {
        return ResponseEntity.ok(bookingService.getSeatMap(projectionId));
    }
}
