package com.cinebh.api.utils;

import java.time.Duration;

public final class BookingSessionDurations {

    public static final Duration SEAT_SELECTION = Duration.ofMinutes(5);
    public static final Duration PAYMENT = Duration.ofMinutes(5);

    private BookingSessionDurations() {
    }
}
