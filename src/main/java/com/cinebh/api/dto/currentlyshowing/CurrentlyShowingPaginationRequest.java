package com.cinebh.api.dto.currentlyshowing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CurrentlyShowingPaginationRequest(
        @Schema(description = "Page index starting from 0", example = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Page size", example = "9")
        @Min(1)
        @Max(50)
        Integer size
) {
    public CurrentlyShowingPaginationRequest {
        if (page == null) {
            page = 0;
        }

        if (size == null) {
            size = 9;
        }
    }
}
