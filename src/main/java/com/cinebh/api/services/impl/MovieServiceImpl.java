package com.cinebh.api.services.impl;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.dto.movie.MovieDetailsResponse;
import com.cinebh.api.dto.movie.MovieProjectionResponse;
import com.cinebh.api.dto.movie.MovieProjectionSearchRequest;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.MovieRepository;
import com.cinebh.api.services.MovieService;
import com.cinebh.api.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    @Override
    public List<HeroMovieResponse> getHeroMovies() {
        return movieRepository.findHeroMovies();
    }

    @Override
    public PageResponse<MovieCardResponse> getCurrentlyShowing(final Integer page, final Integer size) {
        return movieRepository.findCurrentlyShowing(
                PaginationUtils.normalizePage(page),
                PaginationUtils.normalizeSize(size)
        );
    }

    @Override
    public PageResponse<MovieCardResponse> getUpcomingMovies(final Integer page, final Integer size) {
        return movieRepository.findUpcomingMovies(
                PaginationUtils.normalizePage(page),
                PaginationUtils.normalizeSize(size)
        );
    }

    @Override
    public MovieDetailsResponse getMovieDetails(final UUID movieId) {
        return movieRepository.findMovieDetailsById(movieId)
                .orElseThrow(() -> new ApiException("Movie not found.", HttpStatus.NOT_FOUND));
    }

    @Override
    public List<MovieProjectionResponse> getMovieProjections(
            final UUID movieId,
            final MovieProjectionSearchRequest searchRequest
    ) {
        if (!movieRepository.existsById(movieId)) {
            throw new ApiException("Movie not found.", HttpStatus.NOT_FOUND);
        }

        return movieRepository.findMovieProjections(movieId, searchRequest);
    }
}
