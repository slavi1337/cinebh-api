package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.repositories.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private static Stream<Arguments> paginationCases() {
        return Stream.of(
                Arguments.of(null, null, 0, 10),
                Arguments.of(-1, 12, 0, 12),
                Arguments.of(0, 0, 0, 10),
                Arguments.of(2, 100, 2, 50),
                Arguments.of(1, 15, 1, 15)
        );
    }

    @Test
    void shouldReturnHeroMoviesFromRepository() {
        final List<HeroMovieResponse> expectedResponse = List.of(
                new HeroMovieResponse(
                        UUID.randomUUID(),
                        "Avatar",
                        "Synopsis",
                        List.of("Action", "Sci-Fi"),
                        "https://example.com/poster.jpg"
                )
        );

        when(movieRepository.findHeroMovies()).thenReturn(expectedResponse);

        final List<HeroMovieResponse> actualResponse = movieService.getHeroMovies();

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(movieRepository).findHeroMovies();
    }

    @ParameterizedTest
    @MethodSource("paginationCases")
    void shouldNormalizePaginationWhenFetchingCurrentlyShowingMovies(
            final Integer inputPage,
            final Integer inputSize,
            final int expectedPage,
            final int expectedSize
    ) {
        final PageResponse<MovieCardResponse> expectedResponse =
                new PageResponse<>(List.of(), expectedPage, expectedSize, 0, 0);

        when(movieRepository.findCurrentlyShowing(expectedPage, expectedSize)).thenReturn(expectedResponse);

        final PageResponse<MovieCardResponse> actualResponse =
                movieService.getCurrentlyShowing(inputPage, inputSize);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(movieRepository).findCurrentlyShowing(expectedPage, expectedSize);
    }

    @ParameterizedTest
    @MethodSource("paginationCases")
    void shouldNormalizePaginationWhenFetchingUpcomingMovies(
            final Integer inputPage,
            final Integer inputSize,
            final int expectedPage,
            final int expectedSize
    ) {
        final PageResponse<MovieCardResponse> expectedResponse =
                new PageResponse<>(List.of(), expectedPage, expectedSize, 0, 0);

        when(movieRepository.findUpcomingMovies(expectedPage, expectedSize)).thenReturn(expectedResponse);

        final PageResponse<MovieCardResponse> actualResponse =
                movieService.getUpcomingMovies(inputPage, inputSize);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(movieRepository).findUpcomingMovies(expectedPage, expectedSize);
    }
}
