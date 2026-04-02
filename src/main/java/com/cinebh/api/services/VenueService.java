package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.repositories.VenueRepository;
import com.cinebh.api.repositories.projections.VenueCardProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final VenueRepository venueRepository;

    public PageResponse<VenueCardResponse> getVenues(Integer page, Integer size) {
        int resolvedPage = normalizePage(page);
        int resolvedSize = normalizeSize(size);

        Page<VenueCardProjection> result = venueRepository.findAllHomepageVenues(
                PageRequest.of(resolvedPage, resolvedSize)
        );

        List<VenueCardResponse> items = result.getContent()
                .stream()
                .map(this::mapVenueCard)
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private VenueCardResponse mapVenueCard(VenueCardProjection projection) {
        return new VenueCardResponse(
                projection.getId(),
                projection.getName(),
                projection.getAddress(),
                projection.getImageUrl()
        );
    }
}
