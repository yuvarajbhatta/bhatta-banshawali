package com.familytree.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BrandAssetControllerTest {

    @TempDir
    private Path dir;

    @Test
    void returnsTheIconBytesWithLongPublicCaching() throws IOException {
        Path file = dir.resolve("icon.svg");
        Files.write(file, new byte[] {1, 2, 3});
        BrandAssetController controller = new BrandAssetController(file.toString(), "");

        ResponseEntity<byte[]> response = controller.icon();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(1, 2, 3);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.valueOf("image/svg+xml"));
        assertThat(response.getHeaders().getCacheControl()).contains("public").contains("max-age=" + Duration.ofDays(30).toSeconds());
    }

    @Test
    void returnsTheLogoBytesWithLongPublicCaching() throws IOException {
        Path file = dir.resolve("logo.png");
        Files.write(file, new byte[] {4, 5, 6});
        BrandAssetController controller = new BrandAssetController("", file.toString());

        ResponseEntity<byte[]> response = controller.logo();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(4, 5, 6);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    void returnsNotFoundWhenTheConfiguredFileIsMissing() throws IOException {
        BrandAssetController controller = new BrandAssetController(dir.resolve("missing.svg").toString(), dir.resolve("missing.png").toString());

        assertThat(controller.icon().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.logo().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenUnconfigured() throws IOException {
        BrandAssetController controller = new BrandAssetController("", "");

        assertThat(controller.icon().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.logo().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenTheConfiguredPathIsADirectoryNotAFile() throws IOException {
        BrandAssetController controller = new BrandAssetController(dir.toString(), dir.toString());

        assertThat(controller.icon().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.logo().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
