package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.ComponentBuilder;
import com.demcha.compose.loyaut_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for BlockTextBuilder with markdown and bullet offsets.
 */
class BlockTextIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRenderBlockTextWithBulletOffset() throws Exception {
        Path outputFile = tempDir.resolve("block_text_bullet_test.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
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
        Path outputFile = tempDir.resolve("block_text_whitespace_test.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
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
        Path outputFile = tempDir.resolve("block_text_markdown_test.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
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
}
