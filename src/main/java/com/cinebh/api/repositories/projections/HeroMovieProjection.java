package com.cinebh.api.repositories.projections;

import java.util.UUID;

public interface HeroMovieProjection {
    UUID getId();
    String getTitle();
    String getDescription();
    String getGenresCsv();
    String getImageUrl();
}
