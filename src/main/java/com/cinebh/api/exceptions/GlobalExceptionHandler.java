package com.cinebh.api.exceptions;

import com.cinebh.api.dto.common.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(final ApiException exception) {
        log.warn("API exception occurred: status={}, message={}", exception.getStatus(), exception.getMessage());

        final ApiErrorResponse response = new ApiErrorResponse(
                exception.getMessage(),
                exception.getStatus().value(),
                OffsetDateTime.now(),
                exception.getErrors()
        );

        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException exception
    ) {
        log.warn("Method argument validation failed", exception);

        final List<ApiErrorResponse.ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.ValidationError(
                        10001,
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        final ApiErrorResponse response = new ApiErrorResponse(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                OffsetDateTime.now(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(final BindException exception) {
        log.warn("Request binding failed", exception);

        final ApiErrorResponse response = new ApiErrorResponse(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            final ConstraintViolationException exception
    ) {
        log.warn("Constraint violation occurred", exception);

        final ApiErrorResponse response = new ApiErrorResponse(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            ConversionFailedException.class
    })
    public ResponseEntity<ApiErrorResponse> handleRequestParameterConversionException(final Exception exception) {
        log.warn("Request parameter conversion failed", exception);

        final ApiErrorResponse response = new ApiErrorResponse(
                "Invalid request parameter format.",
                HttpStatus.BAD_REQUEST.value(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(final Exception exception) {
        log.error("Unexpected exception occurred", exception);

        final ApiErrorResponse response = new ApiErrorResponse(
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
