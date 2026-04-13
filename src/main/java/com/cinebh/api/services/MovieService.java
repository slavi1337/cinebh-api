package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.repositories.MovieRepository;
import com.cinebh.api.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    public List<HeroMovieResponse> getHeroMovies() {
        return movieRepository.findHeroMovies();
    }

    public PageResponse<MovieCardResponse> getCurrentlyShowing(final Integer page, final Integer size) {
        return movieRepository.findCurrentlyShowing(
                PaginationUtils.normalizePage(page),
                PaginationUtils.normalizeSize(size)
        );
    }

    public PageResponse<MovieCardResponse> getUpcomingMovies(final Integer page, final Integer size) {
        return movieRepository.findUpcomingMovies(
                PaginationUtils.normalizePage(page),
                PaginationUtils.normalizeSize(size)
        );
    }
}
