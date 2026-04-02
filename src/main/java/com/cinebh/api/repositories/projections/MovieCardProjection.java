package com.cinebh.api.repositories.projections;

import java.util.UUID;

public interface MovieCardProjection {
    UUID getId();
    String getTitle();
    Integer getDurationMinutes();
    String getGenresCsv();
    String getCoverImageUrl();
}
