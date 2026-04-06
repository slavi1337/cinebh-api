package com.cinebh.api.services;

import com.cinebh.api.dto.common.PageResponse;
import com.cinebh.api.dto.movie.HeroMovieResponse;
import com.cinebh.api.dto.movie.MovieCardResponse;
import com.cinebh.api.repositories.MovieRepository;
import com.cinebh.api.repositories.projections.HeroMovieProjection;
import com.cinebh.api.repositories.projections.MovieCardProjection;
import com.cinebh.api.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    public List<HeroMovieResponse> getHeroMovies() {
        return movieRepository.findHeroMovies()
                .stream()
                .map(this::mapHeroMovie)
                .toList();
    }

    public PageResponse<MovieCardResponse> getCurrentlyShowing(Integer page, Integer size) {

        final Page<MovieCardProjection> result = movieRepository.findCurrentlyShowing(
                PageRequest.of(
                        PaginationUtils.normalizePage(page),
                        PaginationUtils.normalizeSize(size)
                )
        );

        return mapMoviePage(result);
    }

    public PageResponse<MovieCardResponse> getUpcomingMovies(Integer page, Integer size) {

        final Page<MovieCardProjection> result = movieRepository.findUpcomingMovies(
                PageRequest.of(
                        PaginationUtils.normalizePage(page),
                        PaginationUtils.normalizeSize(size)
                )
        );

        return mapMoviePage(result);
    }

    private HeroMovieResponse mapHeroMovie(HeroMovieProjection projection) {
        return new HeroMovieResponse(
                projection.getId(),
                projection.getTitle(),
                projection.getDescription(),
                splitGenres(projection.getGenresCsv()),
                projection.getImageUrl()
        );
    }

    private PageResponse<MovieCardResponse> mapMoviePage(Page<MovieCardProjection> page) {
        List<MovieCardResponse> items = page.getContent()
                .stream()
                .map(this::mapMovieCard)
                .toList();

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private MovieCardResponse mapMovieCard(MovieCardProjection projection) {
        return new MovieCardResponse(
                projection.getId(),
                projection.getTitle(),
                projection.getDurationMinutes(),
                normalizeGenreLabel(projection.getGenresCsv()),
                projection.getCoverImageUrl()
        );
    }

    private List<String> splitGenres(String genresCsv) {
        if (genresCsv == null || genresCsv.isBlank()) {
            return Collections.emptyList();
        }

        return Stream.of(genresCsv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String normalizeGenreLabel(String genresCsv) {
        if (genresCsv == null || genresCsv.isBlank()) {
            return null;
        }

        return genresCsv;
    }
}
