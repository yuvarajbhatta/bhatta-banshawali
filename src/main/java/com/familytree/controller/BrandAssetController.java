package com.familytree.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * First-party brand assets (favicon, sidebar/login logo) -- fixed,
 * admin-placed files like WatermarkController's image, not user content.
 * Public and unauthenticated: the icon backs the site favicon (fetched
 * by the browser before any page, let alone login, loads) and the logo
 * appears on the pre-login page too.
 */
@RestController
@RequestMapping("/api/v1/brand")
public class BrandAssetController {

    private static final MediaType IMAGE_SVG = MediaType.valueOf("image/svg+xml");

    private final Path iconFile;
    private final Path logoFile;

    public BrandAssetController(
            @Value("${app.brand.icon-file:}") String iconFile,
            @Value("${app.brand.logo-file:}") String logoFile) {
        this.iconFile = iconFile.isBlank() ? null : Path.of(iconFile);
        this.logoFile = logoFile.isBlank() ? null : Path.of(logoFile);
    }

    @GetMapping("/icon")
    public ResponseEntity<byte[]> icon() throws IOException {
        return serve(iconFile, IMAGE_SVG);
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() throws IOException {
        return serve(logoFile, MediaType.IMAGE_PNG);
    }

    private ResponseEntity<byte[]> serve(Path file, MediaType mediaType) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(Files.readAllBytes(file));
    }
}
