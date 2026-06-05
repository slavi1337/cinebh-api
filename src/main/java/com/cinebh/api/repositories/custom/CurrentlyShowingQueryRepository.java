package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.FilterResponse;
import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingFiltersResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingMovieResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingSearchRequest;

import java.util.List;
import java.util.UUID;

public interface CurrentlyShowingQueryRepository {

    PageResponse<CurrentlyShowingMovieResponse> findCurrentlyShowing(
            CurrentlyShowingSearchRequest searchRequest,
            int page,
            int size
    );

    CurrentlyShowingFiltersResponse findFilters();

    List<FilterResponse> findVenuesByCityIds(List<UUID> cityIds);
}
