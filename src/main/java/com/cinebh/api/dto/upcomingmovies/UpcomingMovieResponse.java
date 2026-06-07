package com.cinebh.api.dto.upcomingmovies;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpcomingMovieResponse(
        UUID movieId,
        String title,
        String posterImageUrl,
        Integer durationMinutes,
        List<String> genres,
        List<String> venues,
        LocalDate openingDate
) {
}
