package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingFilterOptionResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMovieResponse;
import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesFiltersResponse;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.UpcomingMoviesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.cinebh.api.support.ControllerTestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UpcomingMoviesControllerTest {

    private static final String UPCOMING_MOVIES_URL = "/api/upcoming-movies";
    private static final String FILTERS_URL = "/api/upcoming-movies/filters";
    private static final String VENUES_FILTER_URL = "/api/upcoming-movies/filters/venues";
    private static final String DEFAULT_QUERY = "dune";

    @Mock
    private UpcomingMoviesService upcomingMoviesService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(new UpcomingMoviesController(upcomingMoviesService));
    }

    @Test
    void shouldReturnUpcomingMoviesWithOkStatus() throws Exception {
        final PageResponse<UpcomingMovieResponse> response = emptyPageResponse();

        when(upcomingMoviesService.getUpcomingMovies(any(), any(), any()))
                .thenReturn(response);

        expectPageResponse(
                mockMvc.perform(getJsonWithPagination(UPCOMING_MOVIES_URL)
                                .param("query", DEFAULT_QUERY))
                        .andExpect(status().isOk())
        );

        verify(upcomingMoviesService).getUpcomingMovies(any(), any(), any());
    }

    @Test
    void shouldReturnFiltersWithOkStatus() throws Exception {
        final UpcomingMoviesFiltersResponse response = new UpcomingMoviesFiltersResponse(
                List.of(new UpcomingFilterOptionResponse(UUID.randomUUID(), "Sarajevo")),
                List.of(new UpcomingFilterOptionResponse(UUID.randomUUID(), "Cinema City")),
                List.of(new UpcomingFilterOptionResponse(UUID.randomUUID(), "Action"))
        );

        when(upcomingMoviesService.getFilters()).thenReturn(response);

        mockMvc.perform(getJson(FILTERS_URL))
                .andExpect(status().isOk());

        verify(upcomingMoviesService).getFilters();
    }

    @Test
    void shouldReturnVenuesByCitiesWithOkStatus() throws Exception {
        final List<UpcomingFilterOptionResponse> response = List.of(
                new UpcomingFilterOptionResponse(UUID.randomUUID(), "Cinema City")
        );

        when(upcomingMoviesService.getVenuesByCities(any())).thenReturn(response);

        mockMvc.perform(getJson(VENUES_FILTER_URL)
                        .param("cityIds", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        verify(upcomingMoviesService).getVenuesByCities(any());
    }

    @Test
    void shouldReturnBadRequestWhenServiceThrowsApiException() throws Exception {
        when(upcomingMoviesService.getUpcomingMovies(any(), any(), any()))
                .thenThrow(new ApiException("Invalid upcoming movies filters", HttpStatus.BAD_REQUEST));

        expectErrorResponse(
                mockMvc.perform(getJsonWithPagination(UPCOMING_MOVIES_URL))
                        .andExpect(status().isBadRequest()),
                "Invalid upcoming movies filters",
                400
        );
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() throws Exception {
        when(upcomingMoviesService.getFilters())
                .thenThrow(new RuntimeException("Unexpected error"));

        expectErrorResponse(
                mockMvc.perform(getJson(FILTERS_URL))
                        .andExpect(status().isInternalServerError()),
                "An unexpected error occurred",
                500
        );
    }
}
