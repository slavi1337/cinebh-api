package com.cinebh.api.dto.profile;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String profileImageUrl,
        UUID cityId,
        String cityName,
        String country,
        String streetAddress
) {
}
