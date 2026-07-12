package com.demcha.compose.engine.components.content;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for {@link ImageData} decoding and the shared
 * {@link ImageSourceCache}: repeated loads of the same source reuse cached bytes
 * and metadata instead of re-decoding. This is the image path the canonical DSL
 * ({@code addImage} / {@code ImageNode}) and the fixed-layout backend rely on.
 */
class ImageDataCacheTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ImageSourceCache.clearForTests();
    }

    @AfterEach
    void tearDown() {
        ImageSourceCache.clearForTests();
    }

    @Test
    void repeatedPathLoadsReuseTheCachedSourceBytes() throws Exception {
        Path imagePath = tempDir.resolve("cached-source.png");
        Files.write(imagePath, pngBytes(120, 60, new Color(24, 92, 160)));

        ImageData firstLoad = ImageData.create(imagePath);

        // Overwrite the file on disk; the cache is keyed by path, so the second
        // load must return the first (cached) bytes and metadata, not re-read.
        Files.write(imagePath, pngBytes(48, 24, new Color(182, 44, 64)));
        ImageData secondLoad = ImageData.create(imagePath);

        assertThat(ImageSourceCache.sourceCacheSize()).isEqualTo(1);
        assertThat(ImageSourceCache.metadataCacheSize()).isEqualTo(1);
        assertThat(firstLoad.getFingerprint()).isEqualTo(secondLoad.getFingerprint());
        assertThat(secondLoad.getMetadata().width()).isEqualTo(120);
        assertThat(secondLoad.getMetadata().height()).isEqualTo(60);
    }

    @Test
    void repeatedByteLoadsDecodeMetadataOnlyOnce() throws Exception {
        byte[] bytes = pngBytes(200, 100, new Color(42, 52, 110));

        ImageData first = ImageData.create(bytes);
        ImageData second = ImageData.create(bytes);

        assertThat(first.getFingerprint()).isEqualTo(second.getFingerprint());
        assertThat(ImageSourceCache.metadataCacheSize()).isEqualTo(1);
        assertThat(ImageSourceCache.metadataDecodeCount()).isEqualTo(1);
    }

    private static byte[] pngBytes(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
