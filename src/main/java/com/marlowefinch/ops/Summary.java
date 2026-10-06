package com.marlowefinch.ops;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The one-line stand-up summary: the four KPIs plus the worst carrier and the busiest
 * ticket category. Both names are null when the range has no data.
 */
public record Summary(
        LocalDate from,
        LocalDate to,
        Double onTimeRate,
        long openTickets,
        BigDecimal revenue,
        long orders,
        String worstCarrier,
        String busiestTicketCategory) {
}
