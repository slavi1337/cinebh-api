package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.venue.VenueCardResponse;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static com.cinebh.api.support.ControllerTestUtils.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VenueControllerTest {

    private static final String VENUES_URL = "/api/v1/venues";

    @Mock
    private VenueService venueService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(new VenueController(venueService));
    }

    @Test
    void shouldReturnVenuesWithOkStatus() throws Exception {
        final PageResponse<VenueCardResponse> response = emptyPageResponse();

        when(venueService.getVenues(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(response);

        expectPageResponse(
                mockMvc.perform(getJsonWithPagination(VENUES_URL))
                        .andExpect(status().isOk())
        );

        verify(venueService).getVenues(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    @Test
    void shouldReturnBadRequestWhenServiceThrowsApiException() throws Exception {
        when(venueService.getVenues(DEFAULT_PAGE, DEFAULT_SIZE))
                .thenThrow(new ApiException("Invalid venue pagination", HttpStatus.BAD_REQUEST));

        expectErrorResponse(
                mockMvc.perform(getJsonWithPagination(VENUES_URL))
                        .andExpect(status().isBadRequest()),
                "Invalid venue pagination",
                400
        );
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() throws Exception {
        when(venueService.getVenues(DEFAULT_PAGE, DEFAULT_SIZE))
                .thenThrow(new RuntimeException("Unexpected error"));

        expectErrorResponse(
                mockMvc.perform(getJsonWithPagination(VENUES_URL))
                        .andExpect(status().isInternalServerError()),
                "An unexpected error occurred",
                500
        );
    }
}
