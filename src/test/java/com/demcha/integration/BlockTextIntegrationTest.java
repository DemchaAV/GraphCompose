package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.engine.components.components_builders.ComponentBuilder;
import com.demcha.compose.engine.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.engine.components.content.text.BlockTextData;
import com.demcha.compose.engine.components.content.text.LineTextData;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test for BlockTextBuilder with markdown and bullet offsets.
 */
class BlockTextIntegrationTest {

    @Test
    void shouldRenderBlockTextWithBulletOffset() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("block_text_bullet_test", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            cb.blockText(Align.left(5), TextStyle.DEFAULT_STYLE)
                    .strategy(BlockIndentStrategy.ALL_LINES)
                    .size(400, 2)
                    .anchor(Anchor.center())
                    .bulletOffset("• ")
                    .padding(Padding.of(5))
                    .margin(Margin.of(5))
                    .text(
                            List.of("**CVRewriter (AI-Powered API Service)** – *Portfolio Project* " +
                                    "Developed a full-stack application centred around a **Spring Boot REST API**."),
                            TextStyle.DEFAULT_STYLE,
                            Padding.of(5),
                            Margin.of(5))
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();
    }

    @Test
    void shouldRenderBlockTextWithWhitespaceIndent() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("block_text_whitespace_test", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            cb.blockText(Align.left(5), TextStyle.DEFAULT_STYLE)
                    .strategy(BlockIndentStrategy.FIRST_LINE)
                    .size(400, 2)
                    .anchor(Anchor.center())
                    .bulletOffset("    ") // 4 spaces indent
                    .padding(Padding.of(5))
                    .margin(Margin.of(5))
                    .text(
                            List.of("This is a paragraph with first-line indent. " +
                                    "The subsequent lines should not have the same indent."),
                            TextStyle.DEFAULT_STYLE,
                            Padding.of(5),
                            Margin.of(5))
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();
    }

    @Test
    void shouldRenderBlockTextWithMarkdownFormatting() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("block_text_markdown_test", "clean", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .markdown(true)
                .guideLines(false)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            cb.blockText(Align.left(3), TextStyle.DEFAULT_STYLE)
                    .size(500, 2)
                    .anchor(Anchor.topLeft())
                    .padding(Padding.of(10))
                    .margin(Margin.of(10))
                    .text(
                            List.of("**Bold text** and *italic text* combined with normal text.\n" +
                                    "This tests markdown parsing in **BlockTextBuilder**."),
                            TextStyle.DEFAULT_STYLE,
                            Padding.zero(),
                            Margin.zero())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();
    }

    @Test
    void shouldIncreaseVerticalGapAroundMarkdownHeading() throws Exception {
        TextStyle style = TextStyle.DEFAULT_STYLE;
        Entity entity;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .markdown(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            entity = cb.blockText(Align.left(2), style)
                    .size(360, 2)
                    .anchor(Anchor.topLeft())
                    .padding(Padding.zero())
                    .margin(Margin.zero())
                    .text(List.of("First line\n# Heading\nThird line"), style, Padding.zero(), Margin.zero())
                    .build();

            composer.build();
        }

        BlockTextData blockTextData = entity.getComponent(BlockTextData.class).orElseThrow();
        assertThat(blockTextData.lines()).hasSize(3);
        assertThat(blockTextData.lines().get(1).hasCachedLineMetrics()).isTrue();
        assertThat(blockTextData.lines().get(1).lineMetrics().lineHeight())
                .isGreaterThan(blockTextData.lines().get(0).lineMetrics().lineHeight());

        double gapBeforeHeading = blockTextData.lines().get(0).y() - blockTextData.lines().get(1).y();
        double gapAfterHeading = blockTextData.lines().get(1).y() - blockTextData.lines().get(2).y();

        double baseStep;
        try (PDDocument fontDocument = new PDDocument()) {
            PdfFont font = (PdfFont) DefaultFonts.library(fontDocument)
                    .getFont(style.fontName(), PdfFont.class)
                    .orElseThrow();
            baseStep = font.getLineHeight(style) + 2.0;
        }

        assertThat(gapBeforeHeading).isGreaterThan(baseStep);
        assertThat(gapAfterHeading).isGreaterThan(baseStep);
    }

    @Test
    void shouldRespectOwnPaddingWhenPositioningBlockTextLines() throws Exception {
        TextStyle style = TextStyle.DEFAULT_STYLE;
        Padding padding = Padding.of(8);
        Entity entity;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .markdown(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            entity = cb.blockText(Align.left(2), style)
                    .size(360, 2)
                    .anchor(Anchor.topLeft())
                    .padding(padding)
                    .margin(Margin.zero())
                    .text(List.of("First line\nSecond line\nThird line"), style, Padding.zero(), Margin.zero())
                    .build();

            composer.build();
        }

        BlockTextData blockTextData = entity.getComponent(BlockTextData.class).orElseThrow();
        Placement placement = entity.getComponent(Placement.class).orElseThrow();

        double baselineOffsetFromBottom;
        try (PDDocument fontDocument = new PDDocument()) {
            PdfFont font = (PdfFont) DefaultFonts.library(fontDocument)
                    .getFont(style.fontName(), PdfFont.class)
                    .orElseThrow();
            baselineOffsetFromBottom = font.verticalMetrics(style).baselineOffsetFromBottom();
        }

        LineTextData firstLine = blockTextData.lines().get(0);
        LineTextData lastLine = blockTextData.lines().get(blockTextData.lines().size() - 1);

        assertThat(firstLine.hasCachedLineWidth()).isTrue();
        assertThat(firstLine.hasCachedLineMetrics()).isTrue();
        assertThat(firstLine.hasCachedBaselineOffset()).isTrue();
        assertThat(lastLine.baselineOffset()).isCloseTo(baselineOffsetFromBottom, within(0.001));
        assertThat(firstLine.x()).isCloseTo(placement.x() + padding.left(), within(0.5));
        assertThat(lastLine.y() - lastLine.baselineOffset())
                .isCloseTo(placement.y() + padding.bottom(), within(0.5));
    }
}

