package com.cinebh.api.dto.currentlyshowing;

import com.cinebh.api.dto.common.FilterResponse;

import java.util.List;

public record CurrentlyShowingFiltersResponse(
        List<FilterResponse> cities,
        List<FilterResponse> venues,
        List<FilterResponse> genres
) {
}
