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

    /** The zero-width non-joiner, built from its code point: a literal would be invisible. */
    private static final String ZWNJ = String.valueOf((char) 0x200C);

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

    @Test
    void aTokenMixingScriptAndDigitsKeepsTheDigitsForwards() throws Exception {
        // "ב-2026" is one whitespace token: a Hebrew letter, a hyphen, and a year.
        // Reversed as one unit the digits come out backwards; split at the direction
        // boundary they read forwards inside the right-to-left line.
        String drawn = glyphsInDrawingOrder(renderRich("ב-2026", TextDirection.RTL));

        assertThat(drawn).contains("2026").doesNotContain("6202");
    }

    @Test
    void autoFixesItsBaseDirectionOncePerParagraph() throws Exception {
        // One paragraph, two logical lines; the first strong character is Hebrew, so
        // the whole paragraph is right-to-left — including the second line, which
        // happens to begin with Latin. Resolved per line instead, that line would take
        // a left-to-right base and arrange itself backwards relative to its sibling.
        byte[] pdf = render(HEBREW + "\nGraphCompose " + HEBREW, TextDirection.AUTO);

        List<TextPosition> secondLine = lineContaining(pdf, 'G');
        double hebrewX = secondLine.stream()
                .filter(pos -> !pos.getUnicode().isEmpty()
                        && pos.getUnicode().charAt(0) >= 'א'
                        && pos.getUnicode().charAt(0) <= 'ת')
                .mapToDouble(TextPosition::getXDirAdj)
                .min()
                .orElseThrow();
        double latinX = secondLine.stream()
                .filter(pos -> "G".equals(pos.getUnicode()))
                .mapToDouble(TextPosition::getXDirAdj)
                .min()
                .orElseThrow();

        assertThat(hebrewX)
                .describedAs("under the paragraph's base direction the Hebrew sits left "
                        + "of the Latin it follows in reading order")
                .isLessThan(latinX);
    }

    private static byte[] renderRich(String text, TextDirection direction) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page
                    .addParagraph(p -> p
                            .rich(com.demcha.compose.document.dsl.RichText.text(text))
                            .direction(direction)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.DAVID_LIBRE)
                                    .size(24)
                                    .build())));

            return document.toPdfBytes();
        }
    }

    /** All glyphs sharing the vertical position of the first occurrence of {@code marker}. */
    private static List<TextPosition> lineContaining(byte[] pdf, char marker) throws IOException {
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
        double markerY = positions.stream()
                .filter(pos -> !pos.getUnicode().isEmpty() && pos.getUnicode().charAt(0) == marker)
                .mapToDouble(TextPosition::getYDirAdj)
                .findFirst()
                .orElseThrow();
        return positions.stream()
                .filter(pos -> Math.abs(pos.getYDirAdj() - markerY) < 1.0)
                .toList();
    }

    @Test
    void arabicReachesThePageInJoinedForms() throws Exception {
        // Joining is a fact about which glyphs were drawn, so it is read from the codes.
        // The reference is the same five letters with the joins suppressed: if the shaper
        // did nothing, the two would draw the same glyphs.
        byte[] joined = renderArabic("مرحبا", TextDirection.RTL);
        byte[] unjoined = renderArabic(
                "م" + ZWNJ + "ر" + ZWNJ + "ح" + ZWNJ + "ب" + ZWNJ + "ا", TextDirection.RTL);

        assertThat(glyphCodesInDrawingOrder(joined))
                .describedAs("the letters are drawn joined, not as the isolated forms the "
                        + "same five letters take when told not to connect")
                .isNotEqualTo(glyphCodesInDrawingOrder(unjoined))
                .describedAs("and all five of them land").hasSize(5);

        assertThat(glyphsInDrawingOrder(joined))
                .describedAs("while what those glyphs stand for is the word as written, "
                        + "read the way the page runs")
                .isEqualTo(new StringBuilder("مرحبا").reverse().toString());
    }

    @Test
    void aParenthesisInHebrewFacesWhatItEncloses() throws Exception {
        // Mirroring swaps the pair at the PDF seam, so the extracted stream carries
        // the swapped characters: the logical '(' before Hebrew is drawn as ')'.
        byte[] pdf = render("(" + HEBREW + ")", TextDirection.RTL);

        String drawn = glyphsInDrawingOrder(pdf);
        assertThat(drawn)
                .describedAs("walking the page left to right, the visually-open side "
                        + "must sit before the Hebrew and face it")
                .startsWith("(");
    }

    private static byte[] renderArabic(String text, TextDirection direction) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text(text)
                            .direction(direction)
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.AMIRI)
                                    .size(24)
                                    .build())));

            return document.toPdfBytes();
        }
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

    /**
     * The page's glyphs themselves, ordered by where they were placed, left to right.
     *
     * <p>A font code says which glyph was drawn; {@code getUnicode} says what the file
     * claims that glyph means, and the two are deliberately different for Arabic — the
     * shaped form is drawn and the letter is what it stands for. Joining can only be
     * observed on this side.</p>
     */
    private static List<Integer> glyphCodesInDrawingOrder(byte[] pdf) throws IOException {
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
        List<Integer> codes = new ArrayList<>();
        for (TextPosition position : positions) {
            for (int code : position.getCharacterCodes()) {
                codes.add(code);
            }
        }
        return codes;
    }

    @Test
    void aZeroWidthNonJoinerWrittenIntoAParagraphReachesTheShaper() throws Exception {
        // The end-to-end half of the joining-control guard. The shaper handles U+200C
        // correctly on its own, but it only ever saw it once control sanitizing stopped
        // deleting it first — a gap no unit test of either piece could show.
        byte[] brokenPdf = renderArabic("ب" + ZWNJ + "ه", TextDirection.RTL);

        assertThat(glyphCodesInDrawingOrder(renderArabic("به", TextDirection.RTL)))
                .describedAs("beh and heh connect when nothing says otherwise")
                .isNotEqualTo(glyphCodesInDrawingOrder(brokenPdf));
        assertThat(glyphsInDrawingOrder(brokenPdf))
                .describedAs("the author's non-joiner has to survive every seam between "
                        + "the builder and the content stream")
                .doesNotContain("‌");
    }
}
