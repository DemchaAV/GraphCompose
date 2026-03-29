package com.demcha.compose.layout_core.components.content;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ImageBuilder;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldReuseCachedBytesForRepeatedPathLoads() throws Exception {
        Path imagePath = tempDir.resolve("cached-source.png");
        Files.write(imagePath, createPngBytes(120, 60, new Color(24, 92, 160)));

        ImageData firstLoad = ImageData.create(imagePath);

        Files.write(imagePath, createPngBytes(48, 24, new Color(182, 44, 64)));

        ImageData secondLoad = ImageData.create(imagePath);

        assertThat(ImageSourceCache.sourceCacheSize()).isEqualTo(1);
        assertThat(ImageSourceCache.metadataCacheSize()).isEqualTo(1);
        assertThat(firstLoad.getFingerprint()).isEqualTo(secondLoad.getFingerprint());
        assertThat(secondLoad.getMetadata().width()).isEqualTo(120);
        assertThat(secondLoad.getMetadata().height()).isEqualTo(60);
    }

    @Test
    void shouldReuseMetadataCacheAndAvoidBuilderRedecode() throws Exception {
        byte[] bytes = createPngBytes(200, 100, new Color(42, 52, 110));

        ImageData first = ImageData.create(bytes);
        ImageData second = ImageData.create(bytes);

        assertThat(first.getFingerprint()).isEqualTo(second.getFingerprint());
        assertThat(ImageSourceCache.metadataCacheSize()).isEqualTo(1);
        assertThat(ImageSourceCache.metadataDecodeCount()).isEqualTo(1);

        try (PdfComposer composer = GraphCompose.pdf().create()) {
            ImageBuilder builder = composer.componentBuilder()
                    .image()
                    .image(first)
                    .fitToBounds(100, 100);

            ContentSize contentSize = builder.build().getComponent(ContentSize.class).orElseThrow();

            assertThat(contentSize.width()).isEqualTo(100);
            assertThat(contentSize.height()).isEqualTo(50);
        }
        assertThat(ImageSourceCache.metadataDecodeCount()).isEqualTo(1);
    }

    private byte[] createPngBytes(int width, int height, Color color) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(color);
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(Color.WHITE);
                graphics.drawRect(0, 0, width - 1, height - 1);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PNG for cache test", e);
        }
    }
}
