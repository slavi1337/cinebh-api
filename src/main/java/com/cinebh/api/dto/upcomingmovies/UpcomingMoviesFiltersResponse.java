package com.cinebh.api.dto.upcomingmovies;

import java.util.List;

public record UpcomingMoviesFiltersResponse(
        List<UpcomingFilterOptionResponse> cities,
        List<UpcomingFilterOptionResponse> venues,
        List<UpcomingFilterOptionResponse> genres
) {
}
