package com.cinebh.api.dto.currentlyshowing;

import java.util.List;

public record CurrentlyShowingFiltersResponse(
        List<FilterOptionResponse> cities,
        List<FilterOptionResponse> venues,
        List<FilterOptionResponse> genres
) {
}
