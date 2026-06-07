package com.cinebh.api.dto.common;

import java.util.UUID;

public record FilterResponse(
        UUID id,
        String label,
        UUID cityId
) {
    public FilterResponse(final UUID id, final String label) {
        this(id, label, null);
    }
}
