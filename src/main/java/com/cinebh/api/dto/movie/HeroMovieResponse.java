package com.cinebh.api.dto.movie;

import java.util.List;
import java.util.UUID;

public record HeroMovieResponse(
        UUID id,
        String title,
        String description,
        List<String> genres,
        String imageUrl
) {
}
