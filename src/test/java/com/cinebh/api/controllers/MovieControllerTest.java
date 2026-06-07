package com.cinebh.api.controllers;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.MovieService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    private static final String HERO_MOVIES_URL = "/api/v1/movies/hero";
    private static final String CURRENTLY_SHOWING_MOVIES_URL = "/api/v1/movies/currently-showing";
    private static final String UPCOMING_MOVIES_URL = "/api/v1/movies/upcoming";

    @Mock
    private MovieService movieService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(new MovieController(movieService));
    }

    @Test
    void shouldReturnHeroMoviesWithOkStatus() throws Exception {
        final List<HeroMovieResponse> response = List.of(
                new HeroMovieResponse(
                        UUID.randomUUID(),
                        "Avatar",
                        "Synopsis",
                        List.of("Fantasy"),
                        "https://example.com/avatar.jpg"
                )
        );

        when(movieService.getHeroMovies()).thenReturn(response);

        mockMvc.perform(getJson(HERO_MOVIES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Avatar"));

        verify(movieService).getHeroMovies();
    }

    @Test
    void shouldReturnCurrentlyShowingMoviesWithOkStatus() throws Exception {
        final PageResponse<MovieCardResponse> response = emptyPageResponse();

        when(movieService.getCurrentlyShowing(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(response);

        expectPageResponse(
                mockMvc.perform(getJsonWithPagination(CURRENTLY_SHOWING_MOVIES_URL))
                        .andExpect(status().isOk())
        );

        verify(movieService).getCurrentlyShowing(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    @Test
    void shouldReturnUpcomingMoviesWithOkStatus() throws Exception {
        final PageResponse<MovieCardResponse> response = emptyPageResponse();

        when(movieService.getUpcomingMovies(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(response);

        expectPageResponse(
                mockMvc.perform(getJsonWithPagination(UPCOMING_MOVIES_URL))
                        .andExpect(status().isOk())
        );

        verify(movieService).getUpcomingMovies(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    @Test
    void shouldReturnBadRequestWhenServiceThrowsApiException() throws Exception {
        when(movieService.getCurrentlyShowing(DEFAULT_PAGE, DEFAULT_SIZE))
                .thenThrow(new ApiException("Invalid pagination parameters", HttpStatus.BAD_REQUEST));

        expectErrorResponse(
                mockMvc.perform(getJsonWithPagination(CURRENTLY_SHOWING_MOVIES_URL))
                        .andExpect(status().isBadRequest()),
                "Invalid pagination parameters",
                400
        );
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() throws Exception {
        when(movieService.getUpcomingMovies(DEFAULT_PAGE, DEFAULT_SIZE))
                .thenThrow(new RuntimeException("Unexpected error"));

        expectErrorResponse(
                mockMvc.perform(getJsonWithPagination(UPCOMING_MOVIES_URL))
                        .andExpect(status().isInternalServerError()),
                "An unexpected error occurred",
                500
        );
    }
}
