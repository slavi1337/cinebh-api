package com.cinebh.api.services;

import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.dto.movie.MovieCastMemberResponse;
import com.cinebh.api.dto.movie.MovieDetailsFilterOptionResponse;
import com.cinebh.api.dto.movie.MovieDetailsResponse;
import com.cinebh.api.dto.movie.MovieProjectionResponse;
import com.cinebh.api.dto.movie.MovieProjectionSearchRequest;
import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.repositories.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieDetailsServiceTest {
    private static final UUID MOVIE_ID = UUID.fromString("00000000-0000-0000-0000-000000000322");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VENUE_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID PROJECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000917");
    private static final UUID HALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
    @Mock
    private MovieRepository movieRepository;
    @InjectMocks
    private MovieService movieService;

    @Test
    void shouldReturnMovieDetailsWhenMovieExists() {
        final MovieDetailsResponse expectedResponse = createMovieDetailsResponse();
        when(movieRepository.findMovieDetailsById(MOVIE_ID))
                .thenReturn(Optional.of(expectedResponse));
        final MovieDetailsResponse actualResponse = movieService.getMovieDetails(MOVIE_ID);
        assertThat(actualResponse).isEqualTo(expectedResponse);
        assertThat(actualResponse.previewImageUrls()).hasSize(4);
        assertThat(actualResponse.seeAlso()).hasSize(1);
        verify(movieRepository).findMovieDetailsById(MOVIE_ID);
    }

    @Test
    void shouldThrowNotFoundWhenMovieDetailsDoNotExist() {
        when(movieRepository.findMovieDetailsById(MOVIE_ID))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> movieService.getMovieDetails(MOVIE_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getMessage()).isEqualTo("Movie not found.");
                });
        verify(movieRepository).findMovieDetailsById(MOVIE_ID);
    }

    @Test
    void shouldReturnMovieProjectionsWhenMovieExists() {
        final MovieProjectionSearchRequest searchRequest = new MovieProjectionSearchRequest(
                LocalDate.of(2026, 5, 25),
                List.of(CITY_ID),
                List.of(VENUE_ID)
        );
        final List<MovieProjectionResponse> expectedResponse = List.of(
                createMovieProjectionResponse()
        );
        when(movieRepository.existsById(MOVIE_ID)).thenReturn(true);
        when(movieRepository.findMovieProjections(MOVIE_ID, searchRequest))
                .thenReturn(expectedResponse);
        final List<MovieProjectionResponse> actualResponse = movieService.getMovieProjections(
                MOVIE_ID,
                searchRequest
        );
        assertThat(actualResponse).isEqualTo(expectedResponse);
        assertThat(actualResponse).hasSize(1);
        assertThat(actualResponse.getFirst().venueName()).isEqualTo("Cineplexx");
        verify(movieRepository).existsById(MOVIE_ID);
        verify(movieRepository).findMovieProjections(MOVIE_ID, searchRequest);
    }

    @Test
    void shouldThrowNotFoundWhenGettingProjectionsForMissingMovie() {
        final MovieProjectionSearchRequest searchRequest = new MovieProjectionSearchRequest(
                LocalDate.of(2026, 5, 25),
                List.of(CITY_ID),
                List.of(VENUE_ID)
        );
        when(movieRepository.existsById(MOVIE_ID)).thenReturn(false);
        assertThatThrownBy(() -> movieService.getMovieProjections(MOVIE_ID, searchRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getMessage()).isEqualTo("Movie not found.");
                });
        verify(movieRepository).existsById(MOVIE_ID);
        verify(movieRepository, never()).findMovieProjections(MOVIE_ID, searchRequest);
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
