package com.demcha.compose.loyaut_core.components.content;

import com.demcha.compose.loyaut_core.components.components_builders.ImageBuilder;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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

    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        ImageSourceCache.clearForTests();
        entityManager = new EntityManager();
        PDDocument document = new PDDocument();
        PdfCanvas canvas = new PdfCanvas(PDRectangle.A4, 0.0f);
        PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, canvas);
        entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystem));
        entityManager.getSystems().addSystem(renderingSystem);
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
    void shouldReuseMetadataCacheAndAvoidBuilderRedecode() {
        byte[] bytes = createPngBytes(200, 100, new Color(42, 52, 110));

        ImageData first = ImageData.create(bytes);
        ImageData second = ImageData.create(bytes);

        assertThat(first.getFingerprint()).isEqualTo(second.getFingerprint());
        assertThat(ImageSourceCache.metadataCacheSize()).isEqualTo(1);
        assertThat(ImageSourceCache.metadataDecodeCount()).isEqualTo(1);

        var entity = new ImageBuilder(entityManager)
                .image(first)
                .fitToBounds(100, 100)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(contentSize.width()).isEqualTo(100);
        assertThat(contentSize.height()).isEqualTo(50);
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
