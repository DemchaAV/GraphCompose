package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.compose.font_library.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for page breaking functionality.
 * Tests that large content correctly spans multiple pages.
 */
class PageBreakerIntegrationTest {

    @Test
    void shouldBreakTextAcrossPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("page_breaker_text_test", "guides", "integration");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            List<Entity> textBlocks = createLargeTextContent(cb, 95, 3);

            var containerBuilder = cb.vContainer(Align.middle(5))
                    .entityName("MainContainer")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(50));

            for (Entity block : textBlocks) {
                containerBuilder.addChild(block);
            }
            containerBuilder.build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        // Verify multiple pages were created
        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void shouldBreakColoredRectanglesAcrossPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("page_breaker_rectangles_test", "guides", "integration");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            List<Entity> rectangles = createColoredRectangles(cb);

            var containerBuilder = cb.vContainer(Align.middle(5))
                    .entityName("MainContainer")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(50));

            for (Entity rect : rectangles) {
                containerBuilder.addChild(rect);
            }
            containerBuilder.build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    private List<Entity> createLargeTextContent(ComponentBuilder cb, int rows, double spacing) {
        List<Entity> data = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i <= rows; i++) {
            sb.append(i).append(". Test text line - We will see how we break our text into pages\n");

            if (i > 0 && i % 20 == 0) {
                data.add(createBlockText(cb, sb.toString(), 302, spacing, "textBlock" + i / 20));
                sb = new StringBuilder();
            }
            if (i == rows && !sb.isEmpty()) {
                data.add(createBlockText(cb, sb.toString(), 302, spacing, "textBlockFinal"));
            }
        }
        return data;
    }

    private Entity createBlockText(ComponentBuilder cb, String text, double width, double spacing, String name) {
        return cb.blockText(Align.middle(spacing), TextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .color(ComponentColor.BLACK)
                .build())
                .size(width, 2)
                .anchor(Anchor.center())
                .padding(Padding.of(5))
                .margin(Margin.of(5))
                .entityName(name)
                .text(List.of(text), TextStyle.DEFAULT_STYLE, Padding.of(5), Margin.of(5))
                .build();
    }

    private List<Entity> createColoredRectangles(ComponentBuilder cb) {
        List<Entity> data = new ArrayList<>();
        List<Color> colors = List.of(Color.BLUE, Color.DARK_GRAY, Color.GREEN, Color.RED, Color.YELLOW);

        for (int i = 0; i < 5; i++) {
            Entity rect = cb.rectangle()
                    .size(300, 300)
                    .stroke(new Stroke(ComponentColor.PURPLE, 2))
                    .fillColor(colors.get(i))
                    .entityName("Rectangle" + i)
                    .build();
            data.add(rect);
        }
        return data;
    }
}

