package com.cinebh.api.dto.upcomingmovies;

import java.util.UUID;

public record UpcomingFilterOptionResponse(
        UUID id,
        String label
) {
}
