package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.repositories.VenueRepository;
import com.cinebh.api.repositories.projections.VenueCardProjection;
import com.cinebh.api.utils.PaginationUtils;
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

    private final VenueRepository venueRepository;

    public PageResponse<VenueCardResponse> getVenues(Integer page, Integer size) {

        final Page<VenueCardProjection> result = venueRepository.findAllHomepageVenues(
                PageRequest.of(
                        PaginationUtils.normalizePage(page),
                        PaginationUtils.normalizeSize(size)
                )
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

    private VenueCardResponse mapVenueCard(VenueCardProjection projection) {
        return new VenueCardResponse(
                projection.getId(),
                projection.getName(),
                projection.getAddress(),
                projection.getImageUrl()
        );
    }
}
