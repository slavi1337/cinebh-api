package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.repositories.VenueRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    @ParameterizedTest
    @MethodSource("com.cinebh.api.support.TestPaginationCases#paginationCases")
    void shouldNormalizePaginationBeforeCallingRepository(
            final Integer inputPage,
            final Integer inputSize,
            final int expectedPage,
            final int expectedSize
    ) {
        final PageResponse<VenueCardResponse> expectedResponse =
                new PageResponse<>(List.of(), expectedPage, expectedSize, 0, 0);

        when(venueRepository.findHomepageVenues(expectedPage, expectedSize)).thenReturn(expectedResponse);

        final PageResponse<VenueCardResponse> actualResponse =
                venueService.getVenues(inputPage, inputSize);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(venueRepository).findHomepageVenues(expectedPage, expectedSize);
    }
}
