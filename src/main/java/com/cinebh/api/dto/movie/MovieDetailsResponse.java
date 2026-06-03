package com.cinebh.api.dto.movie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovieDetailsResponse(
        UUID id,
        String title,
        String synopsis,
        String pgRating,
        String language,
        Integer durationMinutes,
        BigDecimal imdbRating,
        Integer rottenTomatoesRating,
        LocalDate releaseDate,
        LocalDate endDate,
        String trailerUrl,
        String coverImageUrl,
        List<String> previewImageUrls,
        List<String> genres,
        List<MovieCastMemberResponse> cast,
        List<String> directors,
        List<String> writers,
        List<MovieDetailsFilterOptionResponse> cities,
        List<MovieDetailsFilterOptionResponse> venues,
        List<LocalDate> projectionDates,
        List<MovieCardResponse> seeAlso
) {
}
