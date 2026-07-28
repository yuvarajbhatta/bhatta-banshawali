package com.familytree.controller;

import com.familytree.dto.PublicStatsDto;
import com.familytree.services.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context (real SecurityConfig) so this proves /api/v1/public-stats
 * is genuinely reachable without authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:public-stats-controller;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PublicStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @Test
    void getPublicStatsIsReachableWithoutAuthentication() throws Exception {
        when(statsService.getPublicStats()).thenReturn(new PublicStatsDto(120, 6, 1));

        mockMvc.perform(get("/api/v1/public-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentedFamilyMembers").value(120))
                .andExpect(jsonPath("$.documentedGenerations").value(6))
                .andExpect(jsonPath("$.oldestDocumentedGeneration").value(1));
    }
}
