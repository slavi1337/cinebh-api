package com.cinebh.api.dto.movie;

import java.time.LocalTime;
import java.util.UUID;

public record MovieProjectionResponse(
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
