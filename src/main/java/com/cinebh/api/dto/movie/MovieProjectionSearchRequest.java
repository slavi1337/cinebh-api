package com.cinebh.api.dto.movie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovieProjectionSearchRequest(
        @Schema(description = "Selected projection date", example = "2026-04-06")
        LocalDate date,

        @Schema(description = "Selected city IDs")
        List<UUID> cityIds,

        @Schema(description = "Selected venue IDs")
        List<UUID> venueIds
) {
    public MovieProjectionSearchRequest {
        if (date == null) {
            date = LocalDate.now();
        }
    }
}
