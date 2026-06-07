package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.FilterResponse;
import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.common.PaginationRequest;
import com.cinebh.api.dto.currentlyshowing.*;
import com.cinebh.api.services.CurrentlyShowingService;
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
@RequestMapping("/api/v1/currently-showing")
@Tag(name = "Currently Showing", description = "Public endpoints for currently showing movies page")
public class CurrentlyShowingController {

    private final CurrentlyShowingService currentlyShowingService;

    @GetMapping
    @Operation(
            summary = "Get currently showing movies",
            description = "Returns paginated currently showing movies filtered by query, city, venue, genre, date and projection times"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Currently showing movies fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<PageResponse<CurrentlyShowingMovieResponse>> getCurrentlyShowing(
            @Valid @ModelAttribute @ParameterObject CurrentlyShowingSearchRequest searchRequest,
            @Valid @ModelAttribute @ParameterObject CurrentlyShowingPaginationRequest paginationRequest
    ) {
        return ResponseEntity.ok(
                currentlyShowingService.getCurrentlyShowing(
                        searchRequest,
                        paginationRequest.page(),
                        paginationRequest.size()
                )
        );
    }

    @GetMapping("/filters")
    @Operation(
            summary = "Get currently showing filters",
            description = "Returns available cities, venues and genres for currently showing page filters"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Currently showing filters fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<CurrentlyShowingFiltersResponse> getFilters() {
        return ResponseEntity.ok(currentlyShowingService.getFilters());
    }

    @GetMapping("/filters/venues")
    @Operation(
            summary = "Get venues by selected cities",
            description = "Returns available venues filtered by selected city IDs"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered venues fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<List<FilterResponse>> getVenuesByCities(
            @RequestParam(required = false) List<UUID> cityIds
    ) {
        return ResponseEntity.ok(currentlyShowingService.getVenuesByCities(cityIds));
    }
}
