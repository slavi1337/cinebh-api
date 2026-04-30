package com.cinebh.api.repositories.custom;

import com.cinebh.api.dto.currentlyshowing.CurrentlyShowingSearchRequest;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrentlyShowingQueryRepositoryImplTest {

    @Mock
    private JPAQueryFactory queryFactory;

    private CurrentlyShowingQueryRepositoryImpl repository;

    private static Stream<CurrentlyShowingSearchRequest> searchRequests() {
        final UUID cityId = UUID.randomUUID();
        final UUID venueId = UUID.randomUUID();
        final UUID genreId = UUID.randomUUID();

        return Stream.of(
                new CurrentlyShowingSearchRequest(
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        LocalDate.of(2026, 4, 16),
                        List.of()
                ),
                new CurrentlyShowingSearchRequest(
                        "avatar",
                        List.of(cityId),
                        List.of(venueId),
                        List.of(genreId),
                        LocalDate.of(2026, 4, 16),
                        List.of(LocalTime.of(12, 0))
                ),
                new CurrentlyShowingSearchRequest(
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
        repository = new CurrentlyShowingQueryRepositoryImpl(queryFactory);
    }

    @ParameterizedTest
    @MethodSource("searchRequests")
    void shouldBuildPredicateForDifferentSearchRequestCombinations(
            final CurrentlyShowingSearchRequest searchRequest
    ) {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "buildPredicate",
                searchRequest
        );

        assertThat(predicate).isNotNull();
    }

    @Test
    void shouldBuildSelectedDatePredicate() {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "isSelectedDate",
                LocalDate.of(2026, 4, 16)
        );

        assertThat(predicate).isNotNull();
    }

    @Test
    void shouldBuildMovieWindowPredicate() {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "isWithinMovieWindow",
                LocalDate.of(2026, 4, 16)
        );

        assertThat(predicate).isNotNull();
    }

    @Test
    void shouldBuildProjectionTimesPredicate() {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "hasProjectionTimes",
                LocalDate.of(2026, 4, 16),
                List.of(LocalTime.of(12, 0), LocalTime.of(18, 30))
        );

        assertThat(predicate).isNotNull();
    }
}
