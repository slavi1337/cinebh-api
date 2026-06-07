package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.dto.movie.MovieDetailsResponse;
import com.cinebh.api.dto.movie.MovieProjectionResponse;
import com.cinebh.api.dto.movie.MovieProjectionSearchRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieQueryRepository {

    List<HeroMovieResponse> findHeroMovies();

    PageResponse<MovieCardResponse> findCurrentlyShowing(int page, int size);

    PageResponse<MovieCardResponse> findUpcomingMovies(int page, int size);

    Optional<MovieDetailsResponse> findMovieDetailsById(UUID movieId);

    List<MovieProjectionResponse> findMovieProjections(
            UUID movieId,
            MovieProjectionSearchRequest searchRequest
    );
}
