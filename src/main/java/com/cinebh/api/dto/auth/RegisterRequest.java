package com.cinebh.api.dto.auth;

import com.cinebh.api.utils.validation.NotPwned;
import com.cinebh.api.utils.validation.ValidEmailDomain;
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
        @ValidEmailDomain
        String email,

        @Schema(description = "User's password", example = "SecurePass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 48, message = "Password must be between 8 and 48 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
        )
        @NotPwned
        String password
) {
}
