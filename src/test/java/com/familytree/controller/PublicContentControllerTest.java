package com.familytree.controller;

import com.familytree.dto.ArticleDto;
import com.familytree.dto.ArticleSummaryDto;
import com.familytree.services.ContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context (real SecurityConfig, not a slice test with filters
 * disabled) so this genuinely proves /api/v1/content is reachable without
 * authentication, not just that the controller method works in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:public-content-controller;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PublicContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @Test
    void getPublishedArticleIsReachableWithoutAuthentication() throws Exception {
        when(contentService.getPublishedArticle("about-banshawali")).thenReturn(Optional.of(
                new ArticleDto("about-banshawali", "About the Banshawali", "बंशावलीको बारेमा", "English body", "Nepali body", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/v1/content/about-banshawali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("about-banshawali"))
                .andExpect(jsonPath("$.titleEn").value("About the Banshawali"));
    }

    @Test
    void getUnknownOrUnpublishedSlugReturnsNotFound() throws Exception {
        when(contentService.getPublishedArticle("draft-only")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/content/draft-only"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPublishedIsReachableWithoutAuthentication() throws Exception {
        when(contentService.listPublishedArticles()).thenReturn(List.of(
                new ArticleSummaryDto("about-banshawali", "About the Banshawali", "बंशावलीको बारेमा", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/v1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("about-banshawali"));
    }
}
