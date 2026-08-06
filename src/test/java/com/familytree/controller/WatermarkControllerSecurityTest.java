package com.familytree.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context (real SecurityConfig) so this proves
 * GET /api/v1/watermark is genuinely reachable without authentication --
 * it's a decorative site-wide asset shown on every page, including
 * pre-login ones.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:watermark-controller;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class WatermarkControllerSecurityTest {

    @TempDir
    private static Path dir;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void watermarkFile(DynamicPropertyRegistry registry) throws IOException {
        Path file = dir.resolve("watermark.jpg");
        Files.write(file, new byte[] {1, 2, 3});
        registry.add("app.watermark.file", file::toString);
    }

    @Test
    void isReachableWithoutAuthenticationAndCacheableLongTerm() throws Exception {
        mockMvc.perform(get("/api/v1/watermark"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("public")));
    }
}
