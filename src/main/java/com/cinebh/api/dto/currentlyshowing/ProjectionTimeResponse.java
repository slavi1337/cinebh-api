package com.cinebh.api.dto.currentlyshowing;

import java.time.LocalTime;
import java.util.UUID;

public record ProjectionTimeResponse(
        UUID projectionId,
        LocalTime startTime,
        UUID venueId,
        String venueName,
        UUID cityId,
        String cityName,
        UUID hallId,
        String hallName
) {
}
