package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingFilterOptionResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMovieResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesFiltersResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesSearchRequest;

import java.util.List;
import java.util.UUID;

public interface UpcomingMoviesQueryRepository {

    PageResponse<UpcomingMovieResponse> findUpcomingMovies(
            UpcomingMoviesSearchRequest searchRequest,
            int page,
            int size
    );

    UpcomingMoviesFiltersResponse findFilters();

    List<UpcomingFilterOptionResponse> findVenuesByCityIds(List<UUID> cityIds);
}
