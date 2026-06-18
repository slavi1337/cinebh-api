package com.cinebh.api.services;

import com.cinebh.api.dto.ticket.TicketValidationResponse;

import java.util.UUID;

public interface TicketService {

    TicketValidationResponse validateTicket(UUID ticketCode);
}
