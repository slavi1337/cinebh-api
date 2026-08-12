package com.cinebh.api.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 9, max = 47, message = "Password must contain more than 8 and less than 48 characters.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
        )
        String newPassword,

        @NotBlank(message = "Repeat new password is required")
        String repeatNewPassword
) {
}
