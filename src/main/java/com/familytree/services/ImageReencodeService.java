package com.familytree.services;

import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * The only real safety net between an uploaded "photo" and disk, since
 * Picture Album ships without a review queue or a virus scanner
 * (docs/09-security-threat-model.md item 10 -- no ClamAV or similar on
 * this host; this is the documented substitute, not an oversight).
 *
 * Decoding via ImageIO ignores the client's declared Content-Type and
 * file extension entirely -- it only succeeds on bytes that are
 * actually a real raster image. Re-encoding to a fresh JPEG afterwards
 * means only decoded pixel data survives into the stored file; nothing
 * from the original byte stream (embedded scripts, polyglot payloads,
 * arbitrary trailing bytes) passes through.
 */
@Service
public class ImageReencodeService {

    public static final String OUTPUT_MIME_TYPE = "image/jpeg";
    static final int MAX_DIMENSION = 2000;
    // A decompression bomb (tiny compressed file, huge declared pixel
    // dimensions) can make plain ImageIO.read() allocate gigabytes for a
    // multi-KB upload -- checked against the header, which ImageReader
    // parses without decoding pixel data, before the real (memory-costly)
    // decode ever runs. Generous enough for any real camera/phone photo
    // (well above MAX_DIMENSION, which only bounds the *stored* size),
    // small enough to keep worst-case decode memory bounded.
    static final int MAX_SOURCE_DIMENSION = 8000;
    private static final float JPEG_QUALITY = 0.85f;

    public byte[] reencode(byte[] original) {
        BufferedImage decoded = decode(original);
        BufferedImage normalized = flattenToRgb(decoded);
        BufferedImage sized = scaleIfNeeded(normalized);
        return encodeJpeg(sized);
    }

    private BufferedImage decode(byte[] original) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            if (iis == null) {
                throw new IllegalArgumentException("File is not a readable image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("File is not a readable image.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                if (reader.getWidth(0) > MAX_SOURCE_DIMENSION || reader.getHeight(0) > MAX_SOURCE_DIMENSION) {
                    throw new IllegalArgumentException("Image dimensions are too large.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("File is not a readable image.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("File is not a readable image.");
        }
    }

    // JPEG has no alpha channel -- flattening onto a white background
    // here (rather than leaving transparency to whatever the encoder
    // does with it) also normalizes every input color model (indexed,
    // ARGB, grayscale, ...) to plain RGB before scaling/encoding.
    private BufferedImage flattenToRgb(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private BufferedImage scaleIfNeeded(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int longEdge = Math.max(width, height);
        if (longEdge <= MAX_DIMENSION) {
            return image;
        }

        double scale = (double) MAX_DIMENSION / longEdge;
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            // Not a user-input problem -- the runtime is missing a codec
            // this service depends on entirely.
            throw new IllegalStateException("No JPEG writer available in this runtime.");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not re-encode image.");
        } finally {
            writer.dispose();
        }
    }
}
