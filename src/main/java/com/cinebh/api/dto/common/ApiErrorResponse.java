package com.cinebh.api.dto.common;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        String message,
        int status,
        OffsetDateTime timestamp
) {
}
