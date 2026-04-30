package com.cinebh.api.exceptions;

import com.cinebh.api.dto.common.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.ObjectError;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldReturnApiExceptionStatusAndMessage() {
        final ApiException exception = new ApiException("Not found", HttpStatus.NOT_FOUND);

        final ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleApiException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Not found");
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldReturnBadRequestForMethodArgumentNotValidException() throws Exception {
        final Method method = TestController.class.getDeclaredMethod("sampleMethod", String.class);
        final MethodParameter methodParameter = new MethodParameter(method, 0);

        final BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult("value", "sampleMethod");
        bindingResult.addError(new ObjectError("sampleMethod", "Validation failed"));

        final MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        final ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldReturnBadRequestForBindException() {
        final BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult("value", "testObject");
        bindingResult.addError(new ObjectError("testObject", "Validation failed"));

        final BindException exception = new BindException(bindingResult);

        final ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleBindException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldReturnBadRequestForConstraintViolationException() {
        final ConstraintViolationException exception =
                new ConstraintViolationException("Validation failed", null);

        final ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleConstraintViolationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldReturnInternalServerErrorForGenericException() {
        final Exception exception = new Exception("Unexpected failure");

        final ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    private static class TestController {
        public void sampleMethod(final String value) {
        }
    }
}
