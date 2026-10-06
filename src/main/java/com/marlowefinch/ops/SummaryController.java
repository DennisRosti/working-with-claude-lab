package com.marlowefinch.ops;

import java.time.Clock;
import java.util.Comparator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummaryController {

    private final DashboardRepository repository;
    private final Clock clock;

    public SummaryController(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Composes the existing dashboard queries. The worst carrier is the lowest on-time rate
     * among carriers that delivered something; the busiest category has the most tickets
     * opened in the range. Ties go to the alphabetically first name.
     */
    @GetMapping("/api/summary")
    public Summary summary(@RequestParam(required = false) String from,
                           @RequestParam(required = false) String to) {
        DateRange range = DateRange.resolve(from, to, clock);
        Kpis kpis = repository.kpis(range);

        String worstCarrier = repository.onTimeByCarrier(range).stream()
                .filter(c -> c.delivered() > 0)
                .min(Comparator.comparing(CarrierOnTime::rate).thenComparing(CarrierOnTime::carrier))
                .map(CarrierOnTime::carrier)
                .orElse(null);

        String busiestTicketCategory = repository.ticketsByCategory(range).stream()
                .min(Comparator.comparingLong(TicketCategoryCount::total).reversed()
                        .thenComparing(TicketCategoryCount::category))
                .map(TicketCategoryCount::category)
                .orElse(null);

        return new Summary(kpis.from(), kpis.to(), kpis.onTimeRate(), kpis.openTickets(),
                kpis.revenue(), kpis.orders(), worstCarrier, busiestTicketCategory);
    }
}
