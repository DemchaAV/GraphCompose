package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.components.components_builders.ComponentBuilder;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImageIntegrationTest {

    private static final Path COVER_ASSET = Path.of("assets", "GraphComposeCover.png");

    @Test
    void shouldRenderSingleImageWithGuides() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("image_single_guides", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            composer.componentBuilder()
                    .image()
                    .image(createPngBytes(320, 180))
                    .fitToBounds(320, 220)
                    .padding(Padding.of(10))
                    .margin(Margin.of(15))
                    .anchor(Anchor.topCenter())
                    .build();

            composer.build();
        }

        assertPdfExists(outputFile, 1);
    }

    @Test
    void shouldRenderSingleImageWithoutGuides() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("image_single_clean", "clean", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(false)
                .create()) {

            composer.componentBuilder()
                    .image()
                    .image(createPngBytes(320, 180))
                    .fitToBounds(320, 220)
                    .padding(Padding.of(10))
                    .margin(Margin.of(15))
                    .anchor(Anchor.topCenter())
                    .build();

            composer.build();
        }

        assertPdfExists(outputFile, 1);
    }

    @Test
    void shouldRenderImageInsideVContainer() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("image_vcontainer_pagination", "guides", "integration");
        Placement imagePlacement;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            Entity spacer = cb.rectangle()
                    .size(500, 700)
                    .fillColor(ComponentColor.LIGHT_GRAY)
                    .margin(Margin.of(5))
                    .build();

            Entity image = cb.image()
                    .image(createPngBytes(320, 180))
                    .fitToBounds(320, 220)
                    .padding(Padding.of(10))
                    .margin(Margin.of(10))
                    .anchor(Anchor.center())
                    .build();

            cb.vContainer(Align.middle(10))
                    .entityName("ImageStack")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10))
                    .addChild(spacer)
                    .addChild(image)
                    .build();

            composer.build();
            imagePlacement = image.getComponent(Placement.class).orElseThrow();
        }

        assertPdfExists(outputFile, 2);
        assertThat(imagePlacement.startPage()).isEqualTo(imagePlacement.endPage());
        assertThat(imagePlacement.startPage()).isGreaterThan(0);
    }

    @Test
    void shouldRenderFifteenCoverImagesInCenteredVContainer() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("image_vcontainer_cover_column", "guides", "integration");
        assertThat(COVER_ASSET).exists();

        Entity[] images = new Entity[15];
        Placement[] placements = new Placement[15];

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            var container = cb.vContainer(Align.middle(10))
                    .entityName("CoverImageColumn")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10));

            for (int i = 0; i < placements.length; i++) {
                Entity image = cb.image()
                        .image(COVER_ASSET)
                        .scale(0.22)
                        .padding(Padding.of(6))
                        .margin(Margin.of(8))
                        .anchor(Anchor.center())
                        .entityName("CoverImage" + i)
                        .build();
                container.addChild(image);
                images[i] = image;
            }

            container.build();
            composer.build();

            for (int i = 0; i < placements.length; i++) {
                placements[i] = images[i].getComponent(Placement.class).orElseThrow();
            }
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(3);
        }

        for (Placement placement : placements) {
            assertThat(placement.startPage()).isEqualTo(placement.endPage());
        }
    }

    @Test
    void shouldReuseOriginalAndCreateOnlyNeededVariantsInComposer() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            cb.vContainer(Align.middle(10))
                    .entityName("MixedImageSizes")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10))
                    .addChild(cb.image()
                            .image(createPngBytes(200, 100))
                            .fitToBounds(150, 80)
                            .padding(Padding.of(6))
                            .margin(Margin.of(8))
                            .anchor(Anchor.center())
                            .build())
                    .addChild(cb.image()
                            .image(createPngBytes(200, 100))
                            .fitToBounds(40, 20)
                            .padding(Padding.of(6))
                            .margin(Margin.of(8))
                            .anchor(Anchor.center())
                            .build())
                    .addChild(cb.image()
                            .image(createPngBytes(200, 100))
                            .fitToBounds(50, 25)
                            .padding(Padding.of(6))
                            .margin(Margin.of(8))
                            .anchor(Anchor.center())
                            .build())
                    .build();

            composer.toPDDocument();
        }
    }

    private void assertPdfExists(Path outputFile, int minPages) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }

    private byte[] createPngBytes(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(new Color(31, 45, 72));
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(new Color(255, 196, 61));
                graphics.fillRoundRect(width / 8, height / 6, width * 3 / 4, height * 2 / 3, 18, 18);
                graphics.setColor(Color.WHITE);
                graphics.drawRect(0, 0, width - 1, height - 1);
                graphics.drawLine(0, 0, width - 1, height - 1);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create test PNG", e);
        }
    }
}

