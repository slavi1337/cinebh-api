package com.cinebh.api.dto.upcomingmovies;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpcomingMoviesSearchRequest(
        @Schema(description = "Movie title search query", example = "avatar")
        String query,

        @Schema(description = "Selected city IDs")
        List<UUID> cityIds,

        @Schema(description = "Selected venue IDs")
        List<UUID> venueIds,

        @Schema(description = "Selected genre IDs")
        List<UUID> genreIds,

        @Schema(description = "Upcoming movies start date filter", example = "2026-04-15")
        LocalDate startDate,

        @Schema(description = "Upcoming movies end date filter", example = "2026-04-21")
        LocalDate endDate
) {
}
