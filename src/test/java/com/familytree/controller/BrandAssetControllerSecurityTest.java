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
 * GET /api/v1/brand/icon and /logo are genuinely reachable without
 * authentication -- the icon backs the site favicon and the logo shows
 * on the pre-login page too.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:brand-asset-controller;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class BrandAssetControllerSecurityTest {

    @TempDir
    private static Path dir;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void brandFiles(DynamicPropertyRegistry registry) throws IOException {
        Path icon = dir.resolve("icon.svg");
        Files.write(icon, new byte[] {1, 2, 3});
        Path logo = dir.resolve("logo.png");
        Files.write(logo, new byte[] {4, 5, 6});
        registry.add("app.brand.icon-file", icon::toString);
        registry.add("app.brand.logo-file", logo::toString);
    }

    @Test
    void iconIsReachableWithoutAuthenticationAndCacheableLongTerm() throws Exception {
        mockMvc.perform(get("/api/v1/brand/icon"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/svg+xml"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("public")));
    }

    @Test
    void logoIsReachableWithoutAuthenticationAndCacheableLongTerm() throws Exception {
        mockMvc.perform(get("/api/v1/brand/logo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("public")));
    }
}
