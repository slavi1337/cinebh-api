package com.cinebh.api.support;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public final class TestPaginationCases {

    private TestPaginationCases() {
    }

    public static Stream<Arguments> paginationCases() {
        return Stream.of(
                Arguments.of(null, null, 0, 10),
                Arguments.of(-1, 12, 0, 12),
                Arguments.of(0, 0, 0, 10),
                Arguments.of(2, 100, 2, 50),
                Arguments.of(1, 15, 1, 15)
        );
    }
}
