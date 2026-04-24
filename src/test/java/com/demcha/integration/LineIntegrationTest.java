package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.components.components_builders.ComponentBuilder;
import com.demcha.compose.engine.components.content.shape.LinePath;
import com.demcha.compose.engine.components.content.shape.Stroke;
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
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LineIntegrationTest {
    private static final float RENDER_DPI = 144.0f;
    private static final double RENDER_SCALE = RENDER_DPI / 72.0;

    @Test
    void shouldRenderLineVariantsWithoutGuides() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("line_variants", "clean", "integration");

        Entity horizontal;
        Entity vertical;
        Entity diagonal;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .guideLines(false)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            horizontal = cb.line()
                    .horizontal()
                    .size(220, 20)
                    .padding(Padding.of(6))
                    .stroke(new Stroke(ComponentColor.ROYAL_BLUE, 4.0))
                    .anchor(Anchor.topLeft())
                    .build();

            vertical = cb.line()
                    .vertical()
                    .size(20, 120)
                    .padding(Padding.of(6))
                    .stroke(new Stroke(ComponentColor.ORANGE, 4.0))
                    .anchor(Anchor.topLeft())
                    .build();

            diagonal = cb.line()
                    .diagonalDescending()
                    .size(180, 70)
                    .padding(Padding.of(8))
                    .stroke(new Stroke(ComponentColor.DARK_GREEN, 3.0))
                    .anchor(Anchor.topLeft())
                    .build();

            cb.vContainer(Align.left(18))
                    .entityName("LineGallery")
                    .anchor(Anchor.topLeft())
                    .margin(Margin.of(24))
                    .addChild(horizontal)
                    .addChild(vertical)
                    .addChild(diagonal)
                    .build();

            composer.build();
        }

        assertPdfExists(outputFile, 1);

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, RENDER_DPI);

            assertLineIsVisible(image, document, horizontal);
            assertLineIsVisible(image, document, vertical);
            assertLineIsVisible(image, document, diagonal);
        }
    }

    @Test
    void shouldRenderSingleLineWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("line_single_guides", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .guideLines(true)
                .create()) {

            composer.componentBuilder()
                    .line()
                    .diagonalAscending()
                    .size(180, 80)
                    .padding(Padding.of(8))
                    .margin(Margin.of(16))
                    .stroke(new Stroke(ComponentColor.ROYAL_BLUE, 4.0))
                    .anchor(Anchor.topCenter())
                    .build();

            composer.build();
        }

        assertPdfExists(outputFile, 1);
    }

    @Test
    void shouldRenderLineVariantsWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("line_variants_guides", "guides", "integration");
        List<Entity> lines = new ArrayList<>();

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            var container = cb.vContainer(Align.left(18))
                    .entityName("LineGuideGallery")
                    .anchor(Anchor.topLeft())
                    .margin(Margin.of(24));

            lines.add(cb.line()
                    .horizontal()
                    .size(220, 20)
                    .padding(Padding.of(6))
                    .stroke(new Stroke(ComponentColor.ROYAL_BLUE, 4.0))
                    .anchor(Anchor.topLeft())
                    .build());

            lines.add(cb.line()
                    .vertical()
                    .size(20, 120)
                    .padding(Padding.of(6))
                    .stroke(new Stroke(ComponentColor.ORANGE, 4.0))
                    .anchor(Anchor.topLeft())
                    .build());

            lines.add(cb.line()
                    .diagonalDescending()
                    .size(180, 70)
                    .padding(Padding.of(8))
                    .stroke(new Stroke(ComponentColor.DARK_GREEN, 3.0))
                    .anchor(Anchor.topLeft())
                    .build());

            for (Entity line : lines) {
                container.addChild(line);
            }

            container.build();
            composer.build();
        }

        assertPdfExists(outputFile, 1);

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, RENDER_DPI);

            for (Entity line : lines) {
                assertLineIsVisible(image, document, line);
                assertThat(line.getComponent(Placement.class)).isPresent();
            }
        }
    }

    @Test
    void shouldPaginateLineColumnAcrossPagesWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("line_pagination", "guides", "integration");
        List<Entity> lines = new ArrayList<>();

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            var containerBuilder = cb.vContainer(Align.middle(10))
                    .entityName("LinePaginationColumn")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(20));

            for (int i = 0; i < 32; i++) {
                Entity line = cb.line()
                        .size(420, 16)
                        .padding(Padding.of(6))
                        .stroke(new Stroke(i % 2 == 0 ? ComponentColor.ROYAL_BLUE : ComponentColor.DARK_BLUE, 3.0))
                        .margin(Margin.of(8))
                        .anchor(Anchor.center())
                        .entityName("Line" + i)
                        .build();
                lines.add(line);
                containerBuilder.addChild(line);
            }

            containerBuilder.build();
            composer.build();
        }

        assertPdfExists(outputFile, 2);

        int pageCount;
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            pageCount = document.getNumberOfPages();
            assertThat(pageCount).isGreaterThan(1);
        }

        Set<Integer> occupiedPages = new HashSet<>();
        for (Entity line : lines) {
            Placement placement = line.getComponent(Placement.class).orElseThrow();
            assertThat(placement.startPage()).isEqualTo(placement.endPage());
            assertThat(placement.startPage()).isGreaterThanOrEqualTo(0);
            assertThat(placement.endPage()).isLessThan(pageCount);
            occupiedPages.add(placement.startPage());
        }

        assertThat(occupiedPages)
                .as("Lines should be distributed across more than one page")
                .hasSizeGreaterThan(1);
    }

    private void assertPdfExists(Path outputFile, int minPages) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }

    private void assertLineIsVisible(BufferedImage image, PDDocument document, Entity entity) {
        Placement placement = entity.getComponent(Placement.class).orElseThrow();
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        LinePath path = entity.getComponent(LinePath.class).orElseThrow();
        Stroke stroke = entity.getComponent(Stroke.class).orElseThrow();

        double drawableX = placement.x() + padding.left();
        double drawableY = placement.y() + padding.bottom();
        double drawableWidth = Math.max(0.0, placement.width() - padding.horizontal());
        double drawableHeight = Math.max(0.0, placement.height() - padding.vertical());

        int hits = 0;
        for (double t : new double[]{0.05, 0.25, 0.5, 0.75, 0.95}) {
            double sampleX = interpolate(drawableX + drawableWidth * path.startX(), drawableX + drawableWidth * path.endX(), t);
            double sampleY = interpolate(drawableY + drawableHeight * path.startY(), drawableY + drawableHeight * path.endY(), t);

            if (hasColorNear(image, sampleX, sampleY, stroke.strokeColor().color(),
                    Math.max(2, (int) Math.ceil(stroke.width() * RENDER_SCALE)))) {
                hits++;
            }
        }

        assertThat(hits)
                .as("Expected visible line pixels for %s", entity)
                .isGreaterThanOrEqualTo(4);
    }

    private boolean hasColorNear(BufferedImage image,
                                 double pdfX,
                                 double pdfY,
                                 Color expected,
                                 int radius) {
        int centerX = (int) Math.round(pdfX * RENDER_SCALE);
        int centerY = image.getHeight() - 1 - (int) Math.round(pdfY * RENDER_SCALE);

        for (int y = Math.max(0, centerY - radius); y <= Math.min(image.getHeight() - 1, centerY + radius); y++) {
            for (int x = Math.max(0, centerX - radius); x <= Math.min(image.getWidth() - 1, centerX + radius); x++) {
                if (isCloseColor(new Color(image.getRGB(x, y), true), expected, 35)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCloseColor(Color actual, Color expected, int tolerancePerChannel) {
        return Math.abs(actual.getRed() - expected.getRed()) <= tolerancePerChannel
                && Math.abs(actual.getGreen() - expected.getGreen()) <= tolerancePerChannel
                && Math.abs(actual.getBlue() - expected.getBlue()) <= tolerancePerChannel;
    }

    private double interpolate(double start, double end, double t) {
        return start + ((end - start) * t);
    }
}
