package com.cinebh.api.utils;

import com.cinebh.api.entities.Booking;
import com.cinebh.api.entities.BookingSeat;
import com.cinebh.api.entities.SeatTemplate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class BookingSeatUtils {

    private BookingSeatUtils() {
    }

    public static List<BookingSeat> activeSeats(final Booking booking) {
        return booking.getSeats()
                .stream()
                .filter(BookingSeat::isActive)
                .toList();
    }

    public static List<String> activeSeatLabels(final Booking booking) {
        return activeSeats(booking)
                .stream()
                .sorted(bookingSeatComparator())
                .map(BookingSeatUtils::seatLabel)
                .toList();
    }

    public static List<SeatTemplate> sortSeatTemplates(final Collection<SeatTemplate> seatTemplates) {
        return seatTemplates.stream()
                .sorted(seatTemplateComparator())
                .toList();
    }

    public static int parseSeatNumber(final String seatNumber) {
        try {
            return Integer.parseInt(seatNumber);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    public static <T> Comparator<T> seatPositionComparator(
            final Function<T, String> rowExtractor,
            final Function<T, String> seatNumberExtractor
    ) {
        return Comparator
                .comparing(rowExtractor)
                .thenComparingInt(value -> parseSeatNumber(seatNumberExtractor.apply(value)));
    }

    private static Comparator<BookingSeat> bookingSeatComparator() {
        return seatPositionComparator(
                bookingSeat -> bookingSeat.getSeatTemplate().getRowNum(),
                bookingSeat -> bookingSeat.getSeatTemplate().getSeatNum()
        );
    }

    private static Comparator<SeatTemplate> seatTemplateComparator() {
        return seatPositionComparator(SeatTemplate::getRowNum, SeatTemplate::getSeatNum);
    }

    private static String seatLabel(final BookingSeat bookingSeat) {
        return bookingSeat.getSeatTemplate().getRowNum()
                + bookingSeat.getSeatTemplate().getSeatNum();
    }
}
