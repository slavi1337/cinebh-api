package com.cinebh.api.controllers;

import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.dto.movie.MovieCastMemberResponse;
import com.cinebh.api.dto.movie.MovieDetailsFilterOptionResponse;
import com.cinebh.api.dto.movie.MovieDetailsResponse;
import com.cinebh.api.dto.movie.MovieProjectionResponse;
import com.cinebh.api.dto.movie.MovieProjectionSearchRequest;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.exceptions.GlobalExceptionHandler;
import com.cinebh.api.services.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MovieDetailsControllerTest {
    private static final UUID MOVIE_ID = UUID.fromString("00000000-0000-0000-0000-000000000322");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VENUE_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID PROJECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000917");
    private static final UUID HALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    @Mock
    private MovieService movieService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MovieController(movieService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnMovieDetails() throws Exception {
        when(movieService.getMovieDetails(MOVIE_ID))
                .thenReturn(createMovieDetailsResponse());
        mockMvc.perform(get("/api/v1/movies/{movieId}/details", MOVIE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MOVIE_ID.toString()))
                .andExpect(jsonPath("$.title").value("The Mandalorian and Grogu"))
                .andExpect(jsonPath("$.pgRating").value("PG-13"))
                .andExpect(jsonPath("$.language").value("English"))
                .andExpect(jsonPath("$.durationMinutes").value(132))
                .andExpect(jsonPath("$.imdbRating").value(6.5))
                .andExpect(jsonPath("$.rottenTomatoesRating").value(60))
                .andExpect(jsonPath("$.previewImageUrls", hasSize(4)))
                .andExpect(jsonPath("$.genres", hasSize(3)))
                .andExpect(jsonPath("$.cast[0].name").value("Pedro Pascal"))
                .andExpect(jsonPath("$.directors[0]").value("Jon Favreau"))
                .andExpect(jsonPath("$.writers", hasSize(2)))
                .andExpect(jsonPath("$.cities[0].label").value("Sarajevo"))
                .andExpect(jsonPath("$.venues[0].label").value("Cineplexx (Sarajevo)"))
                .andExpect(jsonPath("$.venues[0].cityId").value(CITY_ID.toString()))
                .andExpect(jsonPath("$.projectionDates", hasSize(2)))
                .andExpect(jsonPath("$.seeAlso", hasSize(1)));
        verify(movieService).getMovieDetails(MOVIE_ID);
    }

    @Test
    void shouldReturnNotFoundWhenMovieDetailsDoNotExist() throws Exception {
        when(movieService.getMovieDetails(MOVIE_ID))
                .thenThrow(new ApiException("Movie not found.", HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/api/v1/movies/{movieId}/details", MOVIE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Movie not found."))
                .andExpect(jsonPath("$.status").value(404));
        verify(movieService).getMovieDetails(MOVIE_ID);
    }

    @Test
    void shouldReturnBadRequestForInvalidMovieIdOnDetailsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/movies/{movieId}/details", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter format."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnMovieProjections() throws Exception {
        when(movieService.getMovieProjections(eq(MOVIE_ID), org.mockito.ArgumentMatchers.any(MovieProjectionSearchRequest.class)))
                .thenReturn(List.of(createMovieProjectionResponse()));
        mockMvc.perform(get("/api/v1/movies/{movieId}/projections", MOVIE_ID)
                        .param("date", "2026-05-25")
                        .param("cityIds", CITY_ID.toString())
                        .param("venueIds", VENUE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].projectionId").value(PROJECTION_ID.toString()))
                .andExpect(jsonPath("$[0].startTime").value("17:00:00"))
                .andExpect(jsonPath("$[0].venueId").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$[0].venueName").value("Cineplexx"))
                .andExpect(jsonPath("$[0].cityId").value(CITY_ID.toString()))
                .andExpect(jsonPath("$[0].cityName").value("Sarajevo"))
                .andExpect(jsonPath("$[0].hallId").value(HALL_ID.toString()))
                .andExpect(jsonPath("$[0].hallName").value("Sala 1 - IMAX"));
        final ArgumentCaptor<MovieProjectionSearchRequest> captor =
                ArgumentCaptor.forClass(MovieProjectionSearchRequest.class);
        verify(movieService).getMovieProjections(eq(MOVIE_ID), captor.capture());
        assertThat(captor.getValue().date()).isEqualTo(LocalDate.of(2026, 5, 25));
        assertThat(captor.getValue().cityIds()).containsExactly(CITY_ID);
        assertThat(captor.getValue().venueIds()).containsExactly(VENUE_ID);
    }

    @Test
    void shouldReturnBadRequestForInvalidCityIdOnProjectionsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/movies/{movieId}/projections", MOVIE_ID)
                        .param("date", "2026-05-25")
                        .param("cityIds", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400));
    }

    private MovieDetailsResponse createMovieDetailsResponse() {
        return new MovieDetailsResponse(
                MOVIE_ID,
                "The Mandalorian and Grogu",
                "Din Djarin and Grogu are recruited for a dangerous mission.",
                "PG-13",
                "English",
                132,
                BigDecimal.valueOf(6.5),
                60,
                LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 8, 22),
                "https://www.youtube.com/watch?v=test",
                "https://picsum.photos/seed/mandalorian-cover/600/900",
                List.of(
                        "https://picsum.photos/seed/preview-1/1200/675",
                        "https://picsum.photos/seed/preview-2/1200/675",
                        "https://picsum.photos/seed/preview-3/1200/675",
                        "https://picsum.photos/seed/preview-4/1200/675"
                ),
                List.of("Action", "Adventure", "Sci-Fi"),
                List.of(new MovieCastMemberResponse("Pedro Pascal", "Din Djarin / The Mandalorian")),
                List.of("Jon Favreau"),
                List.of("Jon Favreau", "Dave Filoni"),
                List.of(new MovieDetailsFilterOptionResponse(CITY_ID, "Sarajevo", null)),
                List.of(new MovieDetailsFilterOptionResponse(VENUE_ID, "Cineplexx (Sarajevo)", CITY_ID)),
                List.of(LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 26)),
                List.of(new MovieCardResponse(
                        UUID.fromString("00000000-0000-0000-0000-000000000305"),
                        "Dune: Part Two",
                        166,
                        "Sci-Fi",
                        "https://picsum.photos/seed/dune/600/900"
                ))
        );
    }

    private MovieProjectionResponse createMovieProjectionResponse() {
        return new MovieProjectionResponse(
                PROJECTION_ID,
                LocalTime.of(17, 0),
                VENUE_ID,
                "Cineplexx",
                CITY_ID,
                "Sarajevo",
                HALL_ID,
                "Sala 1 - IMAX"
        );
    }
}
