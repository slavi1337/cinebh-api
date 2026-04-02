package com.cinebh.api.repositories.projections;

import java.util.UUID;

public interface VenueCardProjection {
    UUID getId();
    String getName();
    String getAddress();
    String getImageUrl();
}
