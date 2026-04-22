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
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    private static Stream<Arguments> paginationCases() {
        return Stream.of(
                Arguments.of(null, null, 0, 10),
                Arguments.of(-1, 12, 0, 12),
                Arguments.of(0, 0, 0, 10),
                Arguments.of(2, 100, 2, 50),
                Arguments.of(1, 15, 1, 15)
        );
    }

    @ParameterizedTest
    @MethodSource("paginationCases")
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
