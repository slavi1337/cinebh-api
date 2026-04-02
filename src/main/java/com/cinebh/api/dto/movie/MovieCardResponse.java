package com.cinebh.api.dto.movie;

import java.util.UUID;

public record MovieCardResponse(
        UUID id,
        String title,
        Integer durationMinutes,
        String genreLabel,
        String coverImageUrl
) {
}
