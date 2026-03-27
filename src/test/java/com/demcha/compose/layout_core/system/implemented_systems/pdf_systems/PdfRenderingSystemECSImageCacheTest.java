package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.layout_core.components.content.ImageData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfRenderingSystemECSImageCacheTest {

    @Test
    void shouldReuseOriginalImageXObjectWithinSingleDocument() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            ImageData imageData = ImageData.create(createPngBytes(200, 100));

            PDImageXObject first = renderingSystem.getOrCreateImageXObject(imageData);
            PDImageXObject second = renderingSystem.getOrCreateImageXObject(imageData);

            assertThat(second).isSameAs(first);
            assertThat(renderingSystem.imageCacheStats().originalCount()).isEqualTo(1);
            assertThat(renderingSystem.imageCacheStats().scaledVariantCount()).isEqualTo(0);
        }
    }

    @Test
    void shouldReuseScaledVariantForEquivalentRoundedTargetSizes() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            ImageData imageData = ImageData.create(createPngBytes(200, 100));

            PDImageXObject first = renderingSystem.getOrCreateImageXObject(imageData, 24.2, 16.2);
            PDImageXObject second = renderingSystem.getOrCreateImageXObject(imageData, 24.4, 16.4);

            assertThat(second).isSameAs(first);
            assertThat(renderingSystem.imageCacheStats().originalCount()).isEqualTo(0);
            assertThat(renderingSystem.imageCacheStats().scaledVariantCount()).isEqualTo(1);
        }
    }

    @Test
    void shouldCreateOriginalAndOnlyNeededScaledVariants() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PdfRenderingSystemECS renderingSystem = new PdfRenderingSystemECS(document, new PdfCanvas(PDRectangle.A4, 0.0f));
            ImageData imageData = ImageData.create(createPngBytes(200, 100));

            PDImageXObject original = renderingSystem.getOrCreateImageXObject(imageData, 150, 80);
            PDImageXObject variantSmall = renderingSystem.getOrCreateImageXObject(imageData, 40, 20);
            PDImageXObject variantMedium = renderingSystem.getOrCreateImageXObject(imageData, 50, 25);
            PDImageXObject repeatedSmall = renderingSystem.getOrCreateImageXObject(imageData, 40.2, 20.1);

            assertThat(variantSmall).isSameAs(repeatedSmall);
            assertThat(original).isNotSameAs(variantSmall);
            assertThat(variantMedium).isNotSameAs(variantSmall);
            assertThat(renderingSystem.imageCacheStats().originalCount()).isEqualTo(1);
            assertThat(renderingSystem.imageCacheStats().scaledVariantCount()).isEqualTo(2);
        }
    }

    private byte[] createPngBytes(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(new Color(25, 85, 135));
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(new Color(240, 190, 60));
                graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PNG for PDF image cache test", e);
        }
    }
}
