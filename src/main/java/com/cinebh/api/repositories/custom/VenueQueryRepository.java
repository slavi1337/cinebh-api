package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;

public interface VenueQueryRepository {

    PageResponse<VenueCardResponse> findHomepageVenues(int page, int size);
}
