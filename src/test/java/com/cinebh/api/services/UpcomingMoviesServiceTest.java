package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.common.FilterResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMovieResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesFiltersResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesSearchRequest;
import com.cinebh.api.repositories.UpcomingMoviesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpcomingMoviesServiceTest {

    @Mock
    private UpcomingMoviesRepository upcomingMoviesRepository;

    @InjectMocks
    private UpcomingMoviesService upcomingMoviesService;

    private UpcomingMoviesSearchRequest buildSearchRequest() {
        return new UpcomingMoviesSearchRequest(
                "dune",
                List.of(),
                List.of(),
                List.of(),
                LocalDate.of(2026, 4, 16),
                LocalDate.of(2026, 4, 30)
        );
    }

    @ParameterizedTest
    @MethodSource("com.cinebh.api.support.TestPaginationCases#paginationCases")
    void shouldNormalizePaginationBeforeCallingRepository(
            final Integer inputPage,
            final Integer inputSize,
            final int expectedPage,
            final int expectedSize
    ) {
        final UpcomingMoviesSearchRequest searchRequest = buildSearchRequest();
        final PageResponse<UpcomingMovieResponse> expectedResponse =
                new PageResponse<>(List.of(), expectedPage, expectedSize, 0, 0);

        when(upcomingMoviesRepository.findUpcomingMovies(searchRequest, expectedPage, expectedSize))
                .thenReturn(expectedResponse);

        final PageResponse<UpcomingMovieResponse> actualResponse =
                upcomingMoviesService.getUpcomingMovies(searchRequest, inputPage, inputSize);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(upcomingMoviesRepository).findUpcomingMovies(searchRequest, expectedPage, expectedSize);
    }

    @Test
    void shouldReturnFiltersFromRepository() {
        final UpcomingMoviesFiltersResponse expectedResponse = new UpcomingMoviesFiltersResponse(
                List.of(new FilterResponse(UUID.randomUUID(), "Sarajevo")),
                List.of(new FilterResponse(UUID.randomUUID(), "Cinema City")),
                List.of(new FilterResponse(UUID.randomUUID(), "Action"))
        );

        when(upcomingMoviesRepository.findFilters()).thenReturn(expectedResponse);

        final UpcomingMoviesFiltersResponse actualResponse = upcomingMoviesService.getFilters();

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(upcomingMoviesRepository).findFilters();
    }

    @Test
    void shouldReturnVenuesByCitiesFromRepository() {
        final List<UUID> cityIds = List.of(UUID.randomUUID());

        final List<FilterResponse> expectedResponse = List.of(
                new FilterResponse(UUID.randomUUID(), "Cinema City")
        );

        when(upcomingMoviesRepository.findVenuesByCityIds(cityIds)).thenReturn(expectedResponse);

        final List<FilterResponse> actualResponse =
                upcomingMoviesService.getVenuesByCities(cityIds);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(upcomingMoviesRepository).findVenuesByCityIds(cityIds);
    }
}
