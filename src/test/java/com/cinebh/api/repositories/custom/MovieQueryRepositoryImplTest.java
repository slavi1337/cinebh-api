package com.cinebh.api.repositories.custom;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MovieQueryRepositoryImplTest {

    @Mock
    private JPAQueryFactory queryFactory;

    private MovieQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MovieQueryRepositoryImpl(queryFactory);
    }

    @Test
    void shouldBuildCurrentlyShowingPredicate() {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "isCurrentlyShowing"
        );

        assertThat(predicate).isNotNull();
    }

    @Test
    void shouldBuildUpcomingPredicate() {
        final BooleanExpression predicate = ReflectionTestUtils.invokeMethod(
                repository,
                "isUpcoming"
        );

        assertThat(predicate).isNotNull();
    }

    @Test
    void shouldBuildRandomOrderSpecifier() {
        final OrderSpecifier<Double> orderSpecifier = ReflectionTestUtils.invokeMethod(
                repository,
                "randomOrder"
        );

        assertThat(orderSpecifier).isNotNull();
    }
}
