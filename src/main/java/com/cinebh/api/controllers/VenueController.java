package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.services.VenueService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public PageResponse<VenueCardResponse> getVenues(
            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer page,
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(50)
            Integer size
    ) {
        return venueService.getVenues(page, size);
    }
}
