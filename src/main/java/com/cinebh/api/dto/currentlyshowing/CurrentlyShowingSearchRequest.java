package com.cinebh.api.dto.currentlyshowing;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CurrentlyShowingSearchRequest(
        @Schema(description = "Movie title search query", example = "avatar")
        String query,

        @Schema(description = "Selected city IDs")
        List<UUID> cityIds,

        @Schema(description = "Selected venue IDs")
        List<UUID> venueIds,

        @Schema(description = "Selected genre IDs")
        List<UUID> genreIds,

        @Schema(description = "Selected date", example = "2026-04-06")
        LocalDate date,

        @Schema(description = "Selected projection start times")
        List<LocalTime> projectionTimes
) {
    public CurrentlyShowingSearchRequest {
        if (date == null) {
            date = LocalDate.now();
        }
    }
}
