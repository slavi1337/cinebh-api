package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.dto.movie.MovieDetailsResponse;
import com.cinebh.api.dto.movie.MovieProjectionResponse;
import com.cinebh.api.dto.movie.MovieProjectionSearchRequest;

import java.util.List;
import java.util.UUID;

public interface MovieService {

    List<HeroMovieResponse> getHeroMovies();

    PageResponse<MovieCardResponse> getCurrentlyShowing(Integer page, Integer size);

    PageResponse<MovieCardResponse> getUpcomingMovies(Integer page, Integer size);

    MovieDetailsResponse getMovieDetails(UUID movieId);

    List<MovieProjectionResponse> getMovieProjections(UUID movieId, MovieProjectionSearchRequest searchRequest);
}
