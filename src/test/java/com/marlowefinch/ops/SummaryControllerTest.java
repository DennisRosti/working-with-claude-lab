package com.marlowefinch.ops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** {@code GET /api/summary} through MockMvc against the demo seed data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SummaryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void summaryDefaultsToTheLast30DaysAndNamesWorstCarrierAndBusiestCategory() throws Exception {
        mvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$.from").value("2026-08-22"))
                .andExpect(jsonPath("$.to").value("2026-09-21"))
                .andExpect(jsonPath("$.onTimeRate").value(0.937))
                .andExpect(jsonPath("$.openTickets").value(114))
                .andExpect(jsonPath("$.revenue").value(360095.5))
                .andExpect(jsonPath("$.orders").value(624))
                .andExpect(jsonPath("$.worstCarrier").value("Kessler Logistics"))
                .andExpect(jsonPath("$.busiestTicketCategory").value("Delivery delay"));
    }

    @Test
    void summaryForARangeWithNoDataHasNullNames() throws Exception {
        mvc.perform(get("/api/summary").param("from", "2025-01-01").param("to", "2025-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2025-01-01"))
                .andExpect(jsonPath("$.to").value("2025-01-07"))
                .andExpect(jsonPath("$.onTimeRate").isEmpty())
                .andExpect(jsonPath("$.openTickets").value(0))
                .andExpect(jsonPath("$.revenue").value(0))
                .andExpect(jsonPath("$.orders").value(0))
                .andExpect(jsonPath("$.worstCarrier").isEmpty())
                .andExpect(jsonPath("$.busiestTicketCategory").isEmpty());
    }
}
