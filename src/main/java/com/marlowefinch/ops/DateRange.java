package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;

/**
 * A closed date range for the query endpoints.
 *
 * Both bounds default to "the last 30 days ending today". Request input is validated in
 * {@link #resolve} (via {@link QueryParams}): malformed dates, {@code from} after {@code to}
 * and ranges over 366 days raise {@link InvalidRequestException} (400). The canonical
 * constructor itself does not check ordering. See TODO-232.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int DEFAULT_DAYS = 30;

    public static DateRange resolve(String from, String to, Clock clock) {
        QueryParams params = new QueryParams(clock);
        DateRange range = params.dateRange(from, to);
        params.validate();
        return range;
    }
}
