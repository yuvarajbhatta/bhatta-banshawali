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

class WatermarkControllerTest {

    @TempDir
    private Path dir;

    @Test
    void returnsTheFileBytesWithLongPublicCaching() throws IOException {
        Path file = dir.resolve("watermark.jpg");
        Files.write(file, new byte[] {1, 2, 3});
        WatermarkController controller = new WatermarkController(file.toString());

        ResponseEntity<byte[]> response = controller.get();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(1, 2, 3);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(response.getHeaders().getCacheControl()).contains("public").contains("max-age=" + Duration.ofDays(30).toSeconds());
    }

    @Test
    void returnsNotFoundWhenTheConfiguredFileIsMissing() throws IOException {
        WatermarkController controller = new WatermarkController(dir.resolve("missing.jpg").toString());

        ResponseEntity<byte[]> response = controller.get();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenUnconfigured() throws IOException {
        WatermarkController controller = new WatermarkController("");

        ResponseEntity<byte[]> response = controller.get();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenTheConfiguredPathIsADirectoryNotAFile() throws IOException {
        WatermarkController controller = new WatermarkController(dir.toString());

        ResponseEntity<byte[]> response = controller.get();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
