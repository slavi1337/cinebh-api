package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingFiltersResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingMovieResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingSearchRequest;
import com.cinebh.api.dto.currentlyshowing.FilterOptionResponse;
import com.cinebh.api.repositories.CurrentlyShowingRepository;
import com.cinebh.api.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentlyShowingService {

    private final CurrentlyShowingRepository currentlyShowingRepository;

    public PageResponse<CurrentlyShowingMovieResponse> getCurrentlyShowing(
            CurrentlyShowingSearchRequest searchRequest,
            Integer page,
            Integer size
    ) {
        return currentlyShowingRepository.findCurrentlyShowing(
                searchRequest,
                PaginationUtils.normalizePage(page),
                PaginationUtils.normalizeSize(size)
        );
    }

    public CurrentlyShowingFiltersResponse getFilters() {
        return currentlyShowingRepository.findFilters();
    }

    public List<FilterOptionResponse> getVenuesByCities(List<UUID> cityIds) {
        return currentlyShowingRepository.findVenuesByCityIds(cityIds);
    }
}
