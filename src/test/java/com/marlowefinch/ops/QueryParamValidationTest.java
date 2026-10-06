package com.marlowefinch.ops;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** TODO-232: query parameter validation and the 400 {"errors": [...]} response. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class QueryParamValidationTest {

    static final String BAD_FROM = "from must be an ISO date (YYYY-MM-DD)";
    static final String BAD_TO = "to must be an ISO date (YYYY-MM-DD)";
    static final String FROM_AFTER_TO = "from must be on or before to";
    static final String SPAN_TOO_LONG = "the range from..to may span at most 366 days";
    static final String BAD_LIMIT = "limit must be an integer between 1 and 500";

    @Autowired
    private MockMvc mvc;

    // ---- AC-1: ISO dates ----

    @Test
    void ac1MalformedFromIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "next-tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value(BAD_FROM));
    }

    @Test
    void ac1MalformedToIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-01").param("to", "21/09/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value(BAD_TO));
    }

    @Test
    void ac1ImpossibleCalendarDateIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-02-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(BAD_FROM));
    }

    // ---- AC-2: ordering, span and defaults ----

    @Test
    void ac2FromAfterToIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-21").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value(FROM_AFTER_TO));
    }

    @Test
    void ac2FromEqualToIsAccepted() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-21").param("to", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-09-21"))
                .andExpect(jsonPath("$.to").value("2026-09-21"));
    }

    @Test
    void ac2RangeOf367DaysIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2025-01-01").param("to", "2026-01-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value(SPAN_TOO_LONG));
    }

    @Test
    void ac2RangeOf366DaysIsAccepted() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2025-01-01").param("to", "2026-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2025-01-01"))
                .andExpect(jsonPath("$.to").value("2026-01-02"));
    }

    @Test
    void ac2MissingToDefaultsToToday() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-09-01"))
                .andExpect(jsonPath("$.to").value("2026-09-21"));
    }

    @Test
    void ac2MissingFromDefaultsTo30DaysBeforeToday() throws Exception {
        mvc.perform(get("/api/kpis").param("to", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-08-22"))
                .andExpect(jsonPath("$.to").value("2026-09-21"))
                .andExpect(jsonPath("$.orders").value(624));
    }

    // ---- AC-3: limit ----

    @Test
    void ac3LimitZeroIsRejected() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value(BAD_LIMIT));
    }

    @Test
    void ac3Limit501IsRejected() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(BAD_LIMIT));
    }

    @Test
    void ac3NonNumericLimitIsRejected() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(BAD_LIMIT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "99999999", "99999999999", "1.5"})
    void ac3OtherOutOfRangeLimitsAreRejected(String limit) throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(BAD_LIMIT));
    }

    @Test
    void ac3LimitBoundariesOneAnd500AreAccepted() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-14").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/deliveries/late").param("limit", "500"))
                .andExpect(status().isOk());
    }

    @Test
    void ac3LimitDefaultsTo20() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(20)));
    }

    // ---- AC-4: response shape, several errors, every endpoint ----

    @Test
    void ac4ErrorBodyIsJsonWithAnErrorsArray() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "next-tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"errors\":[\"" + BAD_FROM + "\"]}", true));
    }

    @Test
    void ac4SeveralProblemsProduceSeveralEntries() throws Exception {
        mvc.perform(get("/api/deliveries/late")
                        .param("from", "next-tuesday").param("to", "yesterday").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors", containsInAnyOrder(BAD_FROM, BAD_TO, BAD_LIMIT)));
    }

    @Test
    void ac4FromAfterToAndBadLimitAreBothReported() throws Exception {
        mvc.perform(get("/api/deliveries/late")
                        .param("from", "2026-09-21").param("to", "2026-09-01").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors", containsInAnyOrder(FROM_AFTER_TO, BAD_LIMIT)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void ac4MalformedDateIsRejectedOnEveryRangedEndpoint(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "next-tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value(BAD_FROM));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void ac4FromAfterToIsRejectedOnEveryRangedEndpoint(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "2026-09-21").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(FROM_AFTER_TO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void ac4TooLongRangeIsRejectedOnEveryRangedEndpoint(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "2025-01-01").param("to", "2026-01-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(SPAN_TOO_LONG));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void ac4ValidRequestStillWorksOnEveryRangedEndpoint(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void validRequestStillReturnsTheSeedFigures() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(679))
                .andExpect(jsonPath("$.revenue").value(480209.5));
    }
}
