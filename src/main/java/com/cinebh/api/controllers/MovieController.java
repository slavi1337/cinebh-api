package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.common.PaginationRequest;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.dto.movie.MovieDetailsResponse;
import com.cinebh.api.dto.movie.MovieProjectionResponse;
import com.cinebh.api.dto.movie.MovieProjectionSearchRequest;
import com.cinebh.api.services.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/movies")
@Tag(name = "Movies", description = "Public movie endpoints for homepage, listings and details")
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/hero")
    @Operation(
            summary = "Get hero movies",
            description = "Returns 3 random currently showing movies for homepage hero section"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hero movies fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<HeroMovieResponse>> getHeroMovies() {
        return ResponseEntity.ok(movieService.getHeroMovies());
    }

    @GetMapping("/currently-showing")
    @Operation(
            summary = "Get currently showing movies",
            description = "Returns paginated currently showing movies"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Currently showing movies fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<PageResponse<MovieCardResponse>> getCurrentlyShowing(
            @Valid @ModelAttribute @ParameterObject PaginationRequest paginationRequest
    ) {
        return ResponseEntity.ok(
                movieService.getCurrentlyShowing(
                        paginationRequest.page(),
                        paginationRequest.size()
                )
        );
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "Get upcoming movies",
            description = "Returns paginated upcoming movies"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upcoming movies fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<PageResponse<MovieCardResponse>> getUpcomingMovies(
            @Valid @ModelAttribute @ParameterObject PaginationRequest paginationRequest
    ) {
        return ResponseEntity.ok(
                movieService.getUpcomingMovies(
                        paginationRequest.page(),
                        paginationRequest.size()
                )
        );
    }

    @GetMapping("/{movieId}/details")
    @Operation(
            summary = "Get movie details",
            description = "Returns detailed information needed for the Movie Details page"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie details fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Movie not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<MovieDetailsResponse> getMovieDetails(
            @PathVariable UUID movieId
    ) {
        return ResponseEntity.ok(movieService.getMovieDetails(movieId));
    }

    @GetMapping("/{movieId}/projections")
    @Operation(
            summary = "Get movie projection times",
            description = "Returns projection times for a selected movie, date, city and venue"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie projections fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Movie not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<MovieProjectionResponse>> getMovieProjections(
            @PathVariable UUID movieId,
            @ModelAttribute @ParameterObject MovieProjectionSearchRequest searchRequest
    ) {
        return ResponseEntity.ok(movieService.getMovieProjections(movieId, searchRequest));
    }
}
