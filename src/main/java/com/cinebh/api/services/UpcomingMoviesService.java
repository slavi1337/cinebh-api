package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingFilterOptionResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMovieResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesFiltersResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesSearchRequest;
import com.cinebh.api.repositories.UpcomingMoviesRepository;
import com.cinebh.api.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UpcomingMoviesService {

    private final UpcomingMoviesRepository upcomingMoviesRepository;

    public PageResponse<UpcomingMovieResponse> getUpcomingMovies(
            final UpcomingMoviesSearchRequest searchRequest,
            final Integer page,
            final Integer size
    ) {
        return upcomingMoviesRepository.findUpcomingMovies(
                searchRequest,
                PaginationUtils.normalizePage(page),
                PaginationUtils.normalizeSize(size)
        );
    }

    public UpcomingMoviesFiltersResponse getFilters() {
        return upcomingMoviesRepository.findFilters();
    }

    public List<UpcomingFilterOptionResponse> getVenuesByCities(final List<UUID> cityIds) {
        return upcomingMoviesRepository.findVenuesByCityIds(cityIds);
    }
}
