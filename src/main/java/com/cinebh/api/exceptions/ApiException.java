package com.cinebh.api.exceptions;

import com.cinebh.api.dto.common.ApiErrorResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<ApiErrorResponse.ValidationError> errors;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errors = null;
    }

    public ApiException(String message, HttpStatus status, Integer internalCode, String field, String fieldMessage) {
        super(message);
        this.status = status;
        this.errors = List.of(new ApiErrorResponse.ValidationError(internalCode, field, fieldMessage));
    }
}
