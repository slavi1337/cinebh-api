package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.repositories.VenueRepository;
import com.cinebh.api.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;

    public PageResponse<VenueCardResponse> getVenues(Integer page, Integer size) {
        return venueRepository.findHomepageVenues(
                PaginationUtils.normalizePage(page),
                PaginationUtils.normalizeSize(size)
        );
    }
}
