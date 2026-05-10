package com.cinebh.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "User's email", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "User's password", example = "SecurePass123!")
        @NotBlank(message = "Password is required")
        String password
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
