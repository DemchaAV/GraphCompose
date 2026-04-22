package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for page breaking functionality.
 * Tests that large content correctly spans multiple pages.
 */
class PageBreakerIntegrationTest {

    @Test
    void shouldBreakTextAcrossPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("page_breaker_text_test", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
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
    void shouldPreserveCachedLineMeasurementsAcrossPageBreakCopies() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("page_breaker_block_text_cache_test", "clean", "integration");
        Entity blockText;
        List<Double> widthsBeforeLayout;
        List<Double> baselinesBeforeLayout;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .markdown(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            blockText = cb.blockText(Align.left(3), TextStyle.builder()
                            .fontName(FontName.HELVETICA)
                            .size(9)
                            .color(ComponentColor.BLACK)
                            .build())
                    .size(320, 2)
                    .anchor(Anchor.topLeft())
                    .padding(Padding.of(6))
                    .margin(Margin.of(8))
                    .entityName("PageBreakCacheProbe")
                    .text(List.of(createNestedMarkdownHeadingText(22)),
                            TextStyle.builder()
                                    .fontName(FontName.HELVETICA)
                                    .size(9)
                                    .color(ComponentColor.BLACK)
                                    .build(),
                            Padding.zero(),
                            Margin.zero())
                    .build();

            BlockTextData beforeLayout = blockText.getComponent(BlockTextData.class).orElseThrow();
            widthsBeforeLayout = beforeLayout.lines().stream()
                    .map(LineTextData::lineWidth)
                    .collect(Collectors.toList());
            baselinesBeforeLayout = beforeLayout.lines().stream()
                    .map(LineTextData::baselineOffset)
                    .collect(Collectors.toList());

            composer.build();
        }

        BlockTextData afterLayout = blockText.getComponent(BlockTextData.class).orElseThrow();
        assertThat(afterLayout.lines()).isNotEmpty();
        assertThat(afterLayout.lines()).allSatisfy(line -> {
            assertThat(line.hasCachedLineWidth()).isTrue();
            assertThat(line.hasCachedLineMetrics()).isTrue();
            assertThat(line.hasCachedBaselineOffset()).isTrue();
        });
        assertThat(afterLayout.lines().stream().map(LineTextData::lineWidth).toList())
                .containsExactlyElementsOf(widthsBeforeLayout);
        assertThat(afterLayout.lines().stream().map(LineTextData::baselineOffset).toList())
                .containsExactlyElementsOf(baselinesBeforeLayout);
        assertThat(afterLayout.lines().stream().map(LineTextData::page).distinct().count()).isGreaterThan(1);

        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void shouldBreakColoredRectanglesAcrossPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("page_breaker_rectangles_test", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
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

    @Test
    void shouldBreakNestedContainersWithMarkdownHeadingsAcrossPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf(
                "page_breaker_nested_container_markdown_heading_test",
                "guides",
                "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .markdown(true)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            Entity title = cb.text()
                    .textWithAutoSize("Nested Container Markdown Heading Pagination Preview")
                    .textStyle(TextStyle.builder()
                            .fontName(FontName.HELVETICA)
                            .size(12)
                            .decoration(com.demcha.compose.layout_core.components.content.text.TextDecoration.BOLD)
                            .color(ComponentColor.BLACK)
                            .build())
                    .anchor(Anchor.topLeft())
                    .padding(Padding.zero())
                    .margin(Margin.zero())
                    .entityName("NestedPreviewTitle")
                    .build();

            Entity markdownBlock = cb.blockText(Align.left(2), TextStyle.builder()
                            .fontName(FontName.HELVETICA)
                            .size(9)
                            .color(ComponentColor.BLACK)
                            .build())
                    .size(320, 2)
                    .anchor(Anchor.topLeft())
                    .padding(Padding.of(6))
                    .margin(Margin.of(4))
                    .entityName("NestedMarkdownHeadingBlock")
                    .text(List.of(createNestedMarkdownHeadingText(20)),
                            TextStyle.builder()
                                    .fontName(FontName.HELVETICA)
                                    .size(9)
                                    .color(ComponentColor.BLACK)
                                    .build(),
                            Padding.zero(),
                            Margin.zero())
                    .build();

            Entity innerContainer = cb.vContainer(Align.middle(8))
                    .entityName("InnerMarkdownContainer")
                    .anchor(Anchor.topLeft())
                    .padding(Padding.of(12))
                    .margin(Margin.of(8))
                    .addChild(markdownBlock)
                    .build();

            cb.vContainer(Align.middle(12))
                    .entityName("OuterPreviewContainer")
                    .anchor(Anchor.topCenter())
                    .padding(Padding.of(10))
                    .margin(Margin.of(24))
                    .addChild(title)
                    .addChild(innerContainer)
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void shouldExpandParentPlacementAfterLeafMovesToNextPage() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf(
                "page_breaker_parent_after_leaf_shift_test",
                "guides",
                "integration");

        Placement parentPlacement;
        Placement leafPlacement;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            Entity spacer = cb.rectangle()
                    .size(320, 680)
                    .fillColor(ComponentColor.LIGHT_GRAY)
                    .margin(Margin.of(8))
                    .entityName("PaginationSpacer")
                    .build();

            Entity shiftedLeaf = cb.rectangle()
                    .size(320, 220)
                    .fillColor(ComponentColor.ROYAL_BLUE)
                    .stroke(new Stroke(ComponentColor.DARK_BLUE, 2))
                    .margin(Margin.of(8))
                    .entityName("ShiftedLeaf")
                    .build();

            Entity container = cb.vContainer(Align.middle(10))
                    .entityName("ShiftedLeafContainer")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(24))
                    .addChild(spacer)
                    .addChild(shiftedLeaf)
                    .build();

            composer.build();

            parentPlacement = container.getComponent(Placement.class).orElseThrow();
            leafPlacement = shiftedLeaf.getComponent(Placement.class).orElseThrow();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }

        assertThat(leafPlacement.startPage()).isEqualTo(leafPlacement.endPage());
        assertThat(leafPlacement.startPage()).isGreaterThan(0);

        int parentFirstPage = Math.min(parentPlacement.startPage(), parentPlacement.endPage());
        int parentLastPage = Math.max(parentPlacement.startPage(), parentPlacement.endPage());

        assertThat(parentFirstPage).isEqualTo(0);
        assertThat(parentLastPage).isGreaterThan(parentFirstPage);
        assertThat(leafPlacement.startPage()).isBetween(parentFirstPage, parentLastPage);
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

    private String createNestedMarkdownHeadingText(int sections) {
        String paragraph = "This paragraph lives inside a nested container and is intentionally long so the page breaker " +
                "has to keep recalculating line positions while markdown headings change the local line metrics. " +
                "We want to see that the heading breathes a bit more than the body text and that the container still " +
                "flows correctly across page boundaries without collapsing the reserved height. ";

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= sections; i++) {
            sb.append("# Section ").append(i).append('\n');
            sb.append(paragraph.repeat(3)).append('\n');
            sb.append("Continuation line for section ").append(i)
                    .append(" with **bold text** and *italic text* so markdown token styles stay mixed inside the same block.")
                    .append('\n');
            sb.append("Another continuation line for section ").append(i)
                    .append(" to force wrapping inside the inner container and expose page splitting more clearly.")
                    .append('\n');
        }
        return sb.toString();
    }
}

