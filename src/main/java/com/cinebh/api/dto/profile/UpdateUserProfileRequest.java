package com.cinebh.api.dto.profile;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateUserProfileRequest(
        @Size(max = 50, message = "First name must be at most 50 characters")
        String firstName,

        @Size(max = 50, message = "Last name must be at most 50 characters")
        String lastName,

        String phone,

        UUID cityId,

        @Size(max = 50, message = "Street address must be at most 50 characters")
        String streetAddress
) {
    public UpdateUserProfileRequest {
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
        if (phone != null) {
            phone = phone.trim();
        }
        if (streetAddress != null) {
            streetAddress = streetAddress.trim();
        }
    }
}
