package com.familytree.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context (real SecurityConfig) so this proves
 * /api/v1/date-conversion is genuinely reachable without authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:date-conversion-controller;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class DateConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bsToAdConvertsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/date-conversion/bs-to-ad").param("year", "2080").param("month", "1").param("day", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2023-04-14"));
    }

    @Test
    void adToBsConvertsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/date-conversion/ad-to-bs").param("date", "2023-04-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2080))
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.day").value(1));
    }

    @Test
    void bsToAdRejectsYearOutsideSupportedRangeWithAClearMessage() throws Exception {
        mockMvc.perform(get("/api/v1/date-conversion/bs-to-ad").param("year", "1500").param("month", "1").param("day", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void bsToAdRejectsADayThatDoesNotExistInItsMonth() throws Exception {
        mockMvc.perform(get("/api/v1/date-conversion/bs-to-ad").param("year", "2080").param("month", "1").param("day", "32"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void adToBsRejectsDatesBeforeTheSupportedRange() throws Exception {
        mockMvc.perform(get("/api/v1/date-conversion/ad-to-bs").param("date", "1900-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void bsToAdRejectsNonNumericQueryParams() throws Exception {
        mockMvc.perform(get("/api/v1/date-conversion/bs-to-ad").param("year", "not-a-year").param("month", "1").param("day", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
