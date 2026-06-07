package com.cinebh.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String message,
        int status,
        OffsetDateTime timestamp,
        List<ValidationError> errors
) {
    public ApiErrorResponse(String message, int status, OffsetDateTime timestamp) {
        this(message, status, timestamp, null);
    }

    public record ValidationError(
            Integer internalCode,
            String field,
            String message
    ) {
    }
}
