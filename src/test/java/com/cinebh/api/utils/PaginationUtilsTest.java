package com.cinebh.api.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationUtilsTest {

    @ParameterizedTest
    @NullSource
    void shouldReturnDefaultPageWhenPageIsNull(final Integer page) {
        assertThat(PaginationUtils.normalizePage(page)).isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource({
            "-5, 0",
            "-1, 0",
            "0, 0",
            "1, 1",
            "3, 3"
    })
    void shouldNormalizePageCorrectly(final Integer input, final int expected) {
        assertThat(PaginationUtils.normalizePage(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    void shouldReturnDefaultSizeWhenSizeIsNull(final Integer size) {
        assertThat(PaginationUtils.normalizeSize(size)).isEqualTo(10);
    }

    @ParameterizedTest
    @CsvSource({
            "-5, 10",
            "0, 10",
            "1, 1",
            "10, 10",
            "50, 50",
            "100, 50"
    })
    void shouldNormalizeSizeCorrectly(final Integer input, final int expected) {
        assertThat(PaginationUtils.normalizeSize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 10, 0",
            "1, 10, 1",
            "10, 10, 1",
            "11, 10, 2",
            "20, 10, 2",
            "21, 10, 3",
            "100, 0, 0",
            "100, -1, 0"
    })
    void shouldCalculateTotalPagesCorrectly(final long totalElements, final int size, final int expected) {
        assertThat(PaginationUtils.calculateTotalPages(totalElements, size)).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenUtilityClassConstructorIsCalled() throws Exception {
        final var constructor = PaginationUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("Utility class");
    }
}
