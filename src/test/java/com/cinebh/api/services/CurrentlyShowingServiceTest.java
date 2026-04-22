package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingFiltersResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingMovieResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingSearchRequest;
import com.cinebh.api.dto.currentlyshowing.FilterOptionResponse;
import com.cinebh.api.repositories.CurrentlyShowingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class CurrentlyShowingServiceTest {

    @Mock
    private CurrentlyShowingRepository currentlyShowingRepository;

    @InjectMocks
    private CurrentlyShowingService currentlyShowingService;

    private static Stream<Arguments> paginationCases() {
        return Stream.of(
                Arguments.of(null, null, 0, 10),
                Arguments.of(-1, 12, 0, 12),
                Arguments.of(0, 0, 0, 10),
                Arguments.of(2, 100, 2, 50),
                Arguments.of(1, 15, 1, 15)
        );
    }

    private CurrentlyShowingSearchRequest buildSearchRequest() {
        return new CurrentlyShowingSearchRequest(
                "avatar",
                List.of(),
                List.of(),
                List.of(),
                LocalDate.of(2026, 4, 16),
                List.of(LocalTime.of(12, 0))
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
        final CurrentlyShowingSearchRequest searchRequest = buildSearchRequest();
        final PageResponse<CurrentlyShowingMovieResponse> expectedResponse =
                new PageResponse<>(List.of(), expectedPage, expectedSize, 0, 0);

        when(currentlyShowingRepository.findCurrentlyShowing(searchRequest, expectedPage, expectedSize))
                .thenReturn(expectedResponse);

        final PageResponse<CurrentlyShowingMovieResponse> actualResponse =
                currentlyShowingService.getCurrentlyShowing(searchRequest, inputPage, inputSize);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(currentlyShowingRepository).findCurrentlyShowing(searchRequest, expectedPage, expectedSize);
    }

    @Test
    void shouldReturnFiltersFromRepository() {
        final CurrentlyShowingFiltersResponse expectedResponse = new CurrentlyShowingFiltersResponse(
                List.of(new FilterOptionResponse(UUID.randomUUID(), "Sarajevo")),
                List.of(new FilterOptionResponse(UUID.randomUUID(), "Cinema City")),
                List.of(new FilterOptionResponse(UUID.randomUUID(), "Action"))
        );

        when(currentlyShowingRepository.findFilters()).thenReturn(expectedResponse);

        final CurrentlyShowingFiltersResponse actualResponse = currentlyShowingService.getFilters();

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(currentlyShowingRepository).findFilters();
    }

    @Test
    void shouldReturnVenuesByCitiesFromRepository() {
        final List<UUID> cityIds = List.of(UUID.randomUUID());

        final List<FilterOptionResponse> expectedResponse = List.of(
                new FilterOptionResponse(UUID.randomUUID(), "Cinema City")
        );

        when(currentlyShowingRepository.findVenuesByCityIds(cityIds)).thenReturn(expectedResponse);

        final List<FilterOptionResponse> actualResponse = currentlyShowingService.getVenuesByCities(cityIds);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(currentlyShowingRepository).findVenuesByCityIds(cityIds);
    }
}
