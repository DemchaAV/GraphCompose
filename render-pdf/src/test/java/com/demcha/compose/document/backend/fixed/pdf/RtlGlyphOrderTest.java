package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves right-to-left text reaches the page in the right order, by reading where each
 * glyph was actually placed.
 *
 * <p>This is the assertion the rest of the suite cannot make. A layout snapshot records
 * node geometry and never sees a span, so it cannot tell a reordered line from a
 * logical one; a size-and-header check on the PDF passes either way. Reading the glyph
 * X positions out of the rendered page is direct evidence, and it is exact — no pixel
 * threshold to argue about.</p>
 */
class RtlGlyphOrderTest {

    private static final String HEBREW = "שלום";
    private static final String LATIN = "Hello";

    @Test
    void hebrewIsDrawnRightToLeft() throws Exception {
        String drawn = glyphsInDrawingOrder(render(HEBREW, TextDirection.RTL));

        assertThat(drawn)
                .describedAs("the first letter read is the rightmost letter drawn, so walking "
                        + "the page left to right yields the word reversed")
                .isEqualTo(new StringBuilder(HEBREW).reverse().toString());
    }

    @Test
    void hebrewIsAlsoReorderedWhenTheParagraphIsLeftToRight() throws Exception {
        String drawn = glyphsInDrawingOrder(render(HEBREW, TextDirection.LTR));

        assertThat(drawn)
                .describedAs("the script decides how its own letters run; the paragraph "
                        + "direction only decides what they are embedded in")
                .isEqualTo(new StringBuilder(HEBREW).reverse().toString());
    }

    @Test
    void latinIsUntouched() throws Exception {
        assertThat(glyphsInDrawingOrder(render(LATIN, TextDirection.LTR)))
                .describedAs("the path every existing document takes must be unchanged")
                .isEqualTo(LATIN);
        assertThat(glyphsInDrawingOrder(render(LATIN, TextDirection.RTL)))
                .describedAs("a right-to-left paragraph does not reverse Latin words — "
                        + "only where they sit relative to each other")
                .isEqualTo(LATIN);
    }

    @Test
    void anEmbeddedLatinWordKeepsRunningForwardsInsideHebrew() throws Exception {
        String drawn = glyphsInDrawingOrder(render(HEBREW + " " + LATIN, TextDirection.RTL));

        assertThat(drawn)
                .describedAs("the Latin word reads forwards even though the line runs backwards")
                .contains(LATIN);
        assertThat(drawn.indexOf(LATIN))
                .describedAs("and it sits to the left of the Hebrew it is embedded in")
                .isLessThan(drawn.indexOf(HEBREW.charAt(HEBREW.length() - 1)));
    }

    private static byte[] render(String text, TextDirection direction) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text(text)
                            .direction(direction)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.DAVID_LIBRE)
                                    .size(24)
                                    .build())));

            return document.toPdfBytes();
        }
    }

    /** The page's glyphs, ordered by where they were placed, left to right. */
    private static String glyphsInDrawingOrder(byte[] pdf) throws IOException {
        List<TextPosition> positions = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper collector = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) {
                    positions.addAll(textPositions);
                }
            };
            collector.getText(document);
        }

        positions.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));
        StringBuilder drawn = new StringBuilder();
        for (TextPosition position : positions) {
            drawn.append(position.getUnicode());
        }
        return drawn.toString().trim();
    }
}
