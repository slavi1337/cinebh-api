package com.cinebh.api.dto.movie;

import java.util.UUID;

public record MovieDetailsFilterOptionResponse(
        UUID id,
        String label,
        UUID cityId
) {
}
