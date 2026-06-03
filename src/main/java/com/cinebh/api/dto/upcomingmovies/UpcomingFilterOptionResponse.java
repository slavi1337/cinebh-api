package com.cinebh.api.dto.upcomingmovies;

import java.util.UUID;

public record UpcomingFilterOptionResponse(
        UUID id,
        String label,
        UUID cityId
) {
    public UpcomingFilterOptionResponse(final UUID id, final String label) {
        this(id, label, null);
    }
}
