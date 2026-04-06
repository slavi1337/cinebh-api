package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.common.PaginationRequest;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.services.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/movies")
@Tag(name = "Movies", description = "Public movie endpoints for homepage and listings")
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/hero")
    @Operation(summary = "Get hero movies", description = "Returns 3 random currently showing movies for homepage hero section")
    public ResponseEntity<List<HeroMovieResponse>> getHeroMovies() {
        return ResponseEntity.ok(movieService.getHeroMovies());
    }

    @GetMapping("/currently-showing")
    @Operation(summary = "Get currently showing movies", description = "Returns paginated currently showing movies")
    public ResponseEntity<PageResponse<MovieCardResponse>> getCurrentlyShowing(
            @Valid PaginationRequest paginationRequest
    ) {
        return ResponseEntity.ok(movieService.getCurrentlyShowing(
                        paginationRequest.page(),
                        paginationRequest.size()
                )
        );
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming movies", description = "Returns paginated upcoming movies")
    public ResponseEntity<PageResponse<MovieCardResponse>> getUpcomingMovies(
            @Valid PaginationRequest paginationRequest
    ) {
        return ResponseEntity.ok(movieService.getUpcomingMovies(paginationRequest.page(),
                paginationRequest.size()));
    }
}
