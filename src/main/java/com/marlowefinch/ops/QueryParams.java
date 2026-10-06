package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses and checks the query parameters shared by the dashboard endpoints (TODO-232).
 * Problems are collected rather than thrown one at a time; call {@link #validate()} once
 * everything has been parsed to fail with all of them.
 */
final class QueryParams {

    static final int MAX_RANGE_DAYS = 366;
    static final int MIN_LIMIT = 1;
    static final int MAX_LIMIT = 500;

    private final Clock clock;
    private final List<String> errors = new ArrayList<>();

    QueryParams(Clock clock) {
        this.clock = clock;
    }

    /** {@code from}/{@code to} as a range; missing bounds default to the last 30 days ending today. */
    DateRange dateRange(String from, String to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = isBlank(from) ? today.minusDays(DateRange.DEFAULT_DAYS) : parseDate("from", from);
        LocalDate end = isBlank(to) ? today : parseDate("to", to);
        if (start == null || end == null) {
            return null;
        }
        if (start.isAfter(end)) {
            errors.add("from must be on or before to");
        } else if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            errors.add("the range from..to may span at most " + MAX_RANGE_DAYS + " days");
        }
        return new DateRange(start, end);
    }

    /** {@code limit} as an integer in [1, 500], or {@code defaultValue} when missing. */
    int limit(String limit, int defaultValue) {
        if (isBlank(limit)) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(limit.trim());
            if (value >= MIN_LIMIT && value <= MAX_LIMIT) {
                return value;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        errors.add("limit must be an integer between " + MIN_LIMIT + " and " + MAX_LIMIT);
        return defaultValue;
    }

    /** Throws {@link InvalidRequestException} if any parameter was invalid. */
    void validate() {
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }

    private LocalDate parseDate(String name, String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            errors.add(name + " must be an ISO date (YYYY-MM-DD)");
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
