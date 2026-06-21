package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.InlineSvgRun;
import com.demcha.compose.document.node.InlineTextRun;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage for the {@code emoji(":code:")} DSL: a known shortcode
 * resolves (via the starter {@code graph-compose-emoji} set on the test
 * classpath) to an inline colour glyph; an unknown one degrades to literal text.
 */
class EmojiRenderTest {

    @Test
    void knownShortcodeRendersAsInlineColourGlyph() throws Exception {
        byte[] pdf = render(p -> p.inlineText("Done ").emoji(":white_check_mark:", 14));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document)).contains("Done").doesNotContain("?");
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            // Saturated (non-grey) pixels only reach the page through the emoji
            // glyph — the text is black on white — so finding any proves the
            // shortcode resolved and painted a colour glyph (artwork-independent).
            assertThat(hasSaturatedColour(image))
                    .as("emoji shortcode must resolve to a painted colour glyph")
                    .isTrue();
        }
    }

    @Test
    void secondColourEmojiAlsoResolvesAndPaints() throws Exception {
        byte[] pdf = render(p -> p.inlineText("Launch ").emoji(":rocket:", 14));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document)).contains("Launch").doesNotContain(":rocket:");
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(hasSaturatedColour(image))
                    .as("a second shortcode resolves to a painted colour glyph")
                    .isTrue();
        }
    }

    @Test
    void unknownShortcodeFallsBackToLiteralText() throws Exception {
        byte[] pdf = render(p -> p.inlineText("Ping ").emoji(":not_a_real_emoji:", 14));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document)).contains(":not_a_real_emoji:");
        }
    }

    @Test
    void richTextEmojiResolvesKnownToSvgRunAndUnknownToText() {
        assertThat(RichText.text("").emoji(":star:", 12).runs())
                .anyMatch(InlineSvgRun.class::isInstance);
        assertThat(RichText.text("").emoji(":nope:", 12).runs())
                .anyMatch(run -> run instanceof InlineTextRun text && text.text().equals(":nope:"));
    }

    private static byte[] render(Consumer<ParagraphBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 140)
                .margin(16, 16, 16, 16)
                .create()) {
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(body::accept)
                    .build();
            return session.toPdfBytes();
        }
    }

    /** True if any pixel is a vivid (non-grey, non-near-white/black) colour. */
    private static boolean hasSaturatedColour(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int rr = (rgb >> 16) & 0xFF;
                int gg = (rgb >> 8) & 0xFF;
                int bb = rgb & 0xFF;
                int max = Math.max(rr, Math.max(gg, bb));
                int min = Math.min(rr, Math.min(gg, bb));
                if (max - min > 60 && max > 40) {
                    return true;
                }
            }
        }
        return false;
    }
}
