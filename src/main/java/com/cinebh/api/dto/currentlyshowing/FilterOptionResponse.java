package com.cinebh.api.dto.currentlyshowing;

import java.util.UUID;

public record FilterOptionResponse(
        UUID id,
        String label,
        UUID cityId
) {
    public FilterOptionResponse(final UUID id, final String label) {
        this(id, label, null);
    }
}
