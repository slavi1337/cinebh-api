package com.cinebh.api.dto.currentlyshowing;

import java.util.UUID;

public record FilterOptionResponse(
        UUID id,
        String label
) {
}
