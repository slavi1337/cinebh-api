package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingFilterOptionResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMovieResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesFiltersResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesPaginationRequest;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesSearchRequest;
import com.cinebh.api.services.UpcomingMoviesService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/upcoming-movies")
@Tag(name = "Upcoming Movies", description = "Public endpoints for upcoming movies page")
public class UpcomingMoviesController {

    private final UpcomingMoviesService upcomingMoviesService;

    @GetMapping
    @Operation(
            summary = "Get upcoming movies",
            description = "Returns paginated upcoming movies filtered by query, city, cinema, genre and date range"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upcoming movies fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<PageResponse<UpcomingMovieResponse>> getUpcomingMovies(
            @Valid @ModelAttribute @ParameterObject UpcomingMoviesSearchRequest searchRequest,
            @Valid @ModelAttribute @ParameterObject UpcomingMoviesPaginationRequest paginationRequest
    ) {
        return ResponseEntity.ok(
                upcomingMoviesService.getUpcomingMovies(
                        searchRequest,
                        paginationRequest.page(),
                        paginationRequest.size()
                )
        );
    }

    @GetMapping("/filters")
    @Operation(
            summary = "Get upcoming movies filters",
            description = "Returns available cities, cinemas and genres for upcoming movies filters"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upcoming movie filters fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<UpcomingMoviesFiltersResponse> getFilters() {
        return ResponseEntity.ok(upcomingMoviesService.getFilters());
    }

    @GetMapping("/filters/venues")
    @Operation(
            summary = "Get cinemas by selected cities",
            description = "Returns cinemas filtered by selected city IDs for upcoming movies page"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upcoming movie venues fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<UpcomingFilterOptionResponse>> getVenuesByCities(
            @RequestParam(required = false) List<UUID> cityIds
    ) {
        return ResponseEntity.ok(upcomingMoviesService.getVenuesByCities(cityIds));
    }
}
