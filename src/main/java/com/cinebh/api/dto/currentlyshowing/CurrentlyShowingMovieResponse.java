package com.cinebh.api.dto.currentlyshowing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CurrentlyShowingMovieResponse(
        UUID movieId,
        String title,
        String posterImageUrl,
        String pgRating,
        String language,
        Integer durationMinutes,
        List<String> genres,
        LocalDate endDate,
        List<ProjectionTimeResponse> showtimes
) {
}
