package com.cinebh.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @Schema(description = "User's email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255)
        String email,

        @Schema(description = "User's password", example = "SecurePass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 48, message = "Password must be between 8 and 48 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
        )
        String password
) {
    public RegisterRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
