package com.cinebh.api.utils;

public final class PaginationUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private PaginationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static int normalizePage(final Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    public static int normalizeSize(final Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    public static int calculateTotalPages(final long totalElements, final int size) {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
