package com.cinebh.api.controllers;

import com.cinebh.api.dto.ticket.TicketValidationResponse;
import com.cinebh.api.services.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets", description = "Ticket validation endpoints")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{ticketCode}")
    @Operation(
            summary = "Validate ticket",
            description = "Validates a ticket code and returns booking details only for paid tickets"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket validation result returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ticket code format")
    })
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @PathVariable final UUID ticketCode
    ) {
        return ResponseEntity.ok(ticketService.validateTicket(ticketCode));
    }
}
