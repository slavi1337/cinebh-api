package com.cinebh.api.dto.venue;

import java.util.UUID;

public record VenueCardResponse(
        UUID id,
        String name,
        String address,
        String imageUrl
) {
}
