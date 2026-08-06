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
 * The site-wide decorative background watermark (app.watermark.file) --
 * a single, fixed, admin-placed image, not a Person's photo. Public and
 * unauthenticated (it's a design asset shown on every page, including
 * pre-login ones like /signup) and aggressively cacheable (unlike
 * PersonPhotoController's per-photo "private" caching, there's no
 * per-viewer authorization concern here at all).
 */
@RestController
@RequestMapping("/api/v1/watermark")
public class WatermarkController {

    private final Path watermarkFile;

    public WatermarkController(@Value("${app.watermark.file:}") String watermarkFile) {
        this.watermarkFile = watermarkFile.isBlank() ? null : Path.of(watermarkFile);
    }

    @GetMapping
    public ResponseEntity<byte[]> get() throws IOException {
        if (watermarkFile == null || !Files.isRegularFile(watermarkFile)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(Files.readAllBytes(watermarkFile));
    }
}
