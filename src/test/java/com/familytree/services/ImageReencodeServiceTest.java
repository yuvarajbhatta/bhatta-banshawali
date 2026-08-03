package com.familytree.services;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageReencodeServiceTest {

    private final ImageReencodeService service = new ImageReencodeService();

    @Test
    void reencodesAValidImageToJpeg() throws IOException {
        byte[] input = pngBytes(100, 80, Color.RED);

        byte[] output = service.reencode(input);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));

        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(100);
        assertThat(decoded.getHeight()).isEqualTo(80);
    }

    @Test
    void leavesAnImageWithinTheSizeCapAtItsOriginalDimensions() throws IOException {
        byte[] input = pngBytes(ImageReencodeService.MAX_DIMENSION, 500, Color.BLUE);

        byte[] output = service.reencode(input);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));

        assertThat(decoded.getWidth()).isEqualTo(ImageReencodeService.MAX_DIMENSION);
        assertThat(decoded.getHeight()).isEqualTo(500);
    }

    @Test
    void downscalesAnOversizedImageToTheMaxLongEdge() throws IOException {
        byte[] input = pngBytes(4000, 1000, Color.GREEN);

        byte[] output = service.reencode(input);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));

        assertThat(decoded.getWidth()).isEqualTo(ImageReencodeService.MAX_DIMENSION);
        assertThat(decoded.getHeight()).isEqualTo(500);
    }

    @Test
    void rejectsBytesThatAreNotARealImageRegardlessOfClaimedType() {
        byte[] garbage = "<script>alert(1)</script>".getBytes();

        assertThatThrownBy(() -> service.reencode(garbage))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEmptyPayload() {
        assertThatThrownBy(() -> service.reencode(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outputHasNoAlphaChannelEvenWhenTheSourceDoes() throws IOException {
        BufferedImage argb = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect(0, 0, 20, 20);
        g.dispose();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(argb, "png", buffer);

        byte[] output = service.reencode(buffer.toByteArray());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));

        assertThat(decoded.getColorModel().hasAlpha()).isFalse();
    }

    private byte[] pngBytes(int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        return buffer.toByteArray();
    }
}
