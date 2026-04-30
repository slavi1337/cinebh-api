package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.upcomingmovies.UpcomingMoviesSearchRequest;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UpcomingMoviesQueryRepositoryImplTest {

    @Mock
    private JPAQueryFactory queryFactory;

    private UpcomingMoviesQueryRepositoryImpl repository;

    private static Stream<UpcomingMoviesSearchRequest> searchRequests() {
        final UUID cityId = UUID.randomUUID();
        final UUID venueId = UUID.randomUUID();
        final UUID genreId = UUID.randomUUID();

        return Stream.of(
                new UpcomingMoviesSearchRequest(
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        null
                ),
                new UpcomingMoviesSearchRequest(
                        "dune",
                        List.of(cityId),
                        List.of(venueId),
                        List.of(genreId),
                        LocalDate.of(2026, 4, 16),
                        LocalDate.of(2026, 4, 30)
                ),
                new UpcomingMoviesSearchRequest(
                        "   avatar   ",
                        null,
                        null,
                        null,
                        LocalDate.of(2026, 4, 16),
                        null
                )
        );
    }

    @BeforeEach
    void setUp() {
        repository = new UpcomingMoviesQueryRepositoryImpl(queryFactory);
    }

    @ParameterizedTest
    @MethodSource("searchRequests")
    void shouldBuildPredicateForDifferentSearchRequestCombinations(
            final UpcomingMoviesSearchRequest searchRequest
    ) {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "buildPredicate",
                searchRequest
        );

        assertThat(predicate).isNotNull();
    }

    @Test
    void shouldBuildBaseUpcomingPredicate() {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "baseUpcomingPredicate"
        );

        assertThat(predicate).isNotNull();
    }
}
