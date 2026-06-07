package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.common.PaginationRequest;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.services.VenueService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/venues")
@Tag(name = "Venues", description = "Public venue endpoints for homepage and listings")
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    @Operation(
            summary = "Get paginated venues",
            description = "Returns paginated venues for homepage and listing pages"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venues fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<PageResponse<VenueCardResponse>> getVenues(
            @Valid @ModelAttribute @ParameterObject PaginationRequest paginationRequest
    ) {
        return ResponseEntity.ok(
                venueService.getVenues(
                        paginationRequest.page(),
                        paginationRequest.size()
                )
        );
    }
}
