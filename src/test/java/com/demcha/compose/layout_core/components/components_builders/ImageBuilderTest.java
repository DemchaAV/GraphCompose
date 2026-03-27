package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.content.ImageIntrinsicSize;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ImageBuilderTest {

    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager = new EntityManager();
        PDDocument doc = new PDDocument();
        PdfCanvas canvas = new PdfCanvas(PDRectangle.A4, 0.0f);
        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvas);
        entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
    }

    @Test
    void shouldUseIntrinsicSizeByDefault() {
        Entity entity = new ImageBuilder(entityManager)
                .image(createPngBytes(200, 100))
                .build();

        ImageIntrinsicSize intrinsicSize = entity.getComponent(ImageIntrinsicSize.class).orElseThrow();
        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(intrinsicSize.width()).isEqualTo(200);
        assertThat(intrinsicSize.height()).isEqualTo(100);
        assertThat(contentSize.width()).isEqualTo(200);
        assertThat(contentSize.height()).isEqualTo(100);
    }

    @Test
    void shouldApplyUniformScalePreservingAspectRatio() {
        Entity entity = new ImageBuilder(entityManager)
                .image(createPngBytes(200, 100))
                .scale(0.5)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(contentSize.width()).isCloseTo(100, within(0.001));
        assertThat(contentSize.height()).isCloseTo(50, within(0.001));
    }

    @Test
    void shouldScaleOnlyWidthAxis() {
        Entity entity = new ImageBuilder(entityManager)
                .image(createPngBytes(200, 100))
                .scaleX(1.5)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(contentSize.width()).isCloseTo(300, within(0.001));
        assertThat(contentSize.height()).isCloseTo(100, within(0.001));
    }

    @Test
    void shouldScaleOnlyHeightAxis() {
        Entity entity = new ImageBuilder(entityManager)
                .image(createPngBytes(200, 100))
                .scaleY(2.0)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(contentSize.width()).isCloseTo(200, within(0.001));
        assertThat(contentSize.height()).isCloseTo(200, within(0.001));
    }

    @Test
    void shouldFitWithinBoundsPreservingAspectRatio() {
        Entity entity = new ImageBuilder(entityManager)
                .image(createPngBytes(200, 100))
                .fitToBounds(100, 100)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(contentSize.width()).isCloseTo(100, within(0.001));
        assertThat(contentSize.height()).isCloseTo(50, within(0.001));
    }

    @Test
    void shouldFitToPageAndRespectPadding() {
        PdfCanvas canvas = new PdfCanvas(200, 120, 0, 0);
        canvas.addMargin(Margin.of(10));

        Entity entity = new ImageBuilder(entityManager)
                .image(createPngBytes(300, 150))
                .padding(Padding.of(5))
                .fitToPage(canvas)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(contentSize.width()).isCloseTo(180, within(0.001));
        assertThat(contentSize.height()).isCloseTo(95, within(0.001));
    }

    @Test
    void shouldPreferExplicitSizeOverFitAndScale() {
        Entity entity = new ImageBuilder(entityManager)
                .image(createPngBytes(200, 100))
                .scale(0.5)
                .fitToBounds(50, 50)
                .size(70, 40)
                .build();

        ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();

        assertThat(contentSize.width()).isEqualTo(70);
        assertThat(contentSize.height()).isEqualTo(40);
    }

    private byte[] createPngBytes(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(new Color(32, 40, 88));
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(new Color(255, 214, 10));
                graphics.fillRect(0, 0, width / 2, height / 2);
                graphics.setColor(Color.WHITE);
                graphics.drawRect(0, 0, width - 1, height - 1);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test PNG", e);
        }
    }
}
