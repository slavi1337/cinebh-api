package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingFiltersResponse;
import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingMovieResponse;
import com.cinebh.api.dto.currentlyshowing.FilterOptionResponse;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.CurrentlyShowingService;
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
class CurrentlyShowingControllerTest {

    private static final String CURRENTLY_SHOWING_URL = "/api/v1/currently-showing";
    private static final String FILTERS_URL = "/api/v1/currently-showing/filters";
    private static final String VENUES_FILTER_URL = "/api/v1/currently-showing/filters/venues";
    private static final String DEFAULT_DATE = "2026-04-16";

    @Mock
    private CurrentlyShowingService currentlyShowingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(new CurrentlyShowingController(currentlyShowingService));
    }

    @Test
    void shouldReturnCurrentlyShowingMoviesWithOkStatus() throws Exception {
        final PageResponse<CurrentlyShowingMovieResponse> response = emptyPageResponse();

        when(currentlyShowingService.getCurrentlyShowing(any(), any(), any()))
                .thenReturn(response);

        expectPageResponse(
                mockMvc.perform(getJsonWithPagination(CURRENTLY_SHOWING_URL)
                                .param("date", DEFAULT_DATE))
                        .andExpect(status().isOk())
        );

        verify(currentlyShowingService).getCurrentlyShowing(any(), any(), any());
    }

    @Test
    void shouldReturnBadRequestWhenServiceThrowsApiException() throws Exception {
        when(currentlyShowingService.getCurrentlyShowing(any(), any(), any()))
                .thenThrow(new ApiException("Invalid currently showing filters", HttpStatus.BAD_REQUEST));

        expectErrorResponse(
                mockMvc.perform(getJsonWithPagination(CURRENTLY_SHOWING_URL)
                                .param("date", DEFAULT_DATE))
                        .andExpect(status().isBadRequest()),
                "Invalid currently showing filters",
                400
        );
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() throws Exception {
        when(currentlyShowingService.getCurrentlyShowing(any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        expectErrorResponse(
                mockMvc.perform(getJsonWithPagination(CURRENTLY_SHOWING_URL)
                                .param("date", DEFAULT_DATE))
                        .andExpect(status().isInternalServerError()),
                "An unexpected error occurred",
                500
        );
    }

    @Test
    void shouldReturnFiltersWithOkStatus() throws Exception {
        final CurrentlyShowingFiltersResponse response = new CurrentlyShowingFiltersResponse(
                List.of(new FilterOptionResponse(UUID.randomUUID(), "Sarajevo")),
                List.of(new FilterOptionResponse(UUID.randomUUID(), "Cinema City")),
                List.of(new FilterOptionResponse(UUID.randomUUID(), "Action"))
        );

        when(currentlyShowingService.getFilters()).thenReturn(response);

        mockMvc.perform(getJson(FILTERS_URL))
                .andExpect(status().isOk());

        verify(currentlyShowingService).getFilters();
    }

    @Test
    void shouldReturnVenuesByCitiesWithOkStatus() throws Exception {
        final List<FilterOptionResponse> response = List.of(
                new FilterOptionResponse(UUID.randomUUID(), "Cinema City")
        );

        when(currentlyShowingService.getVenuesByCities(any())).thenReturn(response);

        mockMvc.perform(getJson(VENUES_FILTER_URL)
                        .param("cityIds", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        verify(currentlyShowingService).getVenuesByCities(any());
    }
}
