package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.services.MovieService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/hero")
    public List<HeroMovieResponse> getHeroMovies() {
        return movieService.getHeroMovies();
    }

    @GetMapping("/currently-showing")
    public PageResponse<MovieCardResponse> getCurrentlyShowing(
            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer page,
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(50)
            Integer size
    ) {
        return movieService.getCurrentlyShowing(page, size);
    }

    @GetMapping("/upcoming")
    public PageResponse<MovieCardResponse> getUpcomingMovies(
            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer page,
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(50)
            Integer size
    ) {
        return movieService.getUpcomingMovies(page, size);
    }
}
