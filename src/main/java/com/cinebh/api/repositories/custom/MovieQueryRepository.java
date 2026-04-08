package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;

import java.util.List;

public interface MovieQueryRepository {

    List<HeroMovieResponse> findHeroMovies();

    PageResponse<MovieCardResponse> findCurrentlyShowing(int page, int size);

    PageResponse<MovieCardResponse> findUpcomingMovies(int page, int size);
}
