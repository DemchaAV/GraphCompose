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
            // The green disc (#5C913B) only reaches the page through the emoji
            // glyph, so green pixels prove the shortcode resolved and painted.
            assertThat(containsColorNear(image, 92, 145, 59, 40))
                    .as("emoji shortcode must resolve to a painted colour glyph")
                    .isTrue();
        }
    }

    @Test
    void gradientEmojiPaintsItsShading() throws Exception {
        byte[] pdf = render(p -> p.inlineText("Status ").emoji(":purple_circle:", 14));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 129, 80, 224, 60))
                    .as("gradient emoji must paint its violet shading")
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

    private static boolean containsColorNear(BufferedImage image, int r, int g, int b, int tolerance) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int rr = (rgb >> 16) & 0xFF;
                int gg = (rgb >> 8) & 0xFF;
                int bb = rgb & 0xFF;
                if (Math.abs(rr - r) <= tolerance
                        && Math.abs(gg - g) <= tolerance
                        && Math.abs(bb - b) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }
}
