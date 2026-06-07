package com.cinebh.api.dto.upcomingmovies;

import com.cinebh.api.dto.common.FilterResponse;

import java.util.List;

public record UpcomingMoviesFiltersResponse(
        List<FilterResponse> cities,
        List<FilterResponse> venues,
        List<FilterResponse> genres
) {
}
