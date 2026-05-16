package com.cinebh.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

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
        String password,

        @Schema(description = "User's first name", example = "John")
        @Size(max = 50, message = "First name must be at most 50 characters")
        String firstName,

        @Schema(description = "User's last name", example = "Doe")
        @Size(max = 50, message = "Last name must be at most 50 characters")
        String lastName,

        @Schema(description = "International mobile phone number", example = "+38761123456")
        String phone,

        @Schema(description = "Profile image URL", example = "https://placehold.co/600x400")
        @Pattern(
                regexp = "^https?://[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(?::\\d{1,5})?(?:/[^\\s]*)?$",
                message = "Must be a valid URL"
        )
        String profileImageUrl,

        @Schema(description = "City ID")
        UUID cityId,

        @Schema(description = "Street address", example = "Marsala Tita 12")
        @Size(max = 50, message = "Street address must be at most 50 characters")
        String streetAddress
) {
    public RegisterRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
