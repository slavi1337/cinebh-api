package com.cinebh.api.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PaginationRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(50)
        Integer size
) {
    public PaginationRequest {
        if (page == null) {
            page = 0;
        }

        if (size == null) {
            size = 10;
        }
    }
}
