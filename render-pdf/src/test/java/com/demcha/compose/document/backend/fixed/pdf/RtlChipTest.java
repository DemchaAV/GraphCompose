package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Holds a highlight chip to the words it wraps, in a line that gets reordered.
 *
 * <p>A chip is a fill painted behind a run, and it is the third thing on a line whose
 * position is arithmetic rather than a consequence of where the glyphs went — after the link
 * rectangle and the decoration mark. It is also the most visible of the three if it lands
 * wrong: a coloured box behind the wrong words is not a subtle defect, but it only appears
 * once a line is reordered, which a left-to-right page never does.</p>
 *
 * <p>The fill is measured by its colour. The chip is painted in a hue nothing else on the
 * page uses, so its extent can be read straight off the raster without diffing two renders —
 * which matters here, because a chip changes the line's metrics, and the same run with and
 * without one is not the same layout.</p>
 *
 * <p>The last case is about what is <em>inside</em> the chip. A chip is one unsplittable
 * span, so it is reversed whole or not at all; that is a deliberate approximation, and the
 * part of it that has to hold is that a chip of right-to-left text is in fact reversed.</p>
 *
 * <p>Rendered at 72 dpi, so a pixel is a point and the fill's extent compares with the glyph
 * positions the text extractor reports without a scale factor in between.</p>
 */
class RtlChipTest {

    /**
     * A Latin chip inside Hebrew, and the chip is first in logical order.
     *
     * <p>Latin because Hebrew words share letters, so a glyph cannot be attributed to a word
     * by what it is; first because in a right-to-left line the first logical run is drawn at
     * the right edge, which is the widest the two orders can disagree.</p>
     */
    private static final String CHIP = "GRAPH";
    private static final String REST = " שלום עולם ועוד מילים";

    /** A hue nothing else on the page uses, so the fill can be found by colour alone. */
    private static final DocumentColor FILL = DocumentColor.rgb(220, 0, 0);

    private static final double TOLERANCE = 2.0;

    /** Dark enough to be a glyph rather than an antialiased edge. */
    private static final int INK = 110;

    @Test
    void theChipSitsBehindTheWordsItWrapsInARightToLeftLine() throws Exception {
        byte[] pdf = render(TextDirection.RTL, CHIP, REST);
        Extent chip = fillExtent(pdf);
        Span wrapped = drawnSpan(pdf, RtlChipTest::isLatin);
        Span hebrew = drawnSpan(pdf, RtlChipTest::isHebrew);

        assertThat(chip.left())
                .describedAs("the fill must be painted where the chip's own glyphs are drawn, "
                        + "not where they would sit in a left-to-right line. Fill %s, wrapped "
                        + "word %.1f..%.1f, the other words at %.1f..%.1f",
                        chip, wrapped.left(), wrapped.right(), hebrew.left(), hebrew.right())
                .isLessThan(wrapped.left() + TOLERANCE);
        assertThat(chip.right())
                .describedAs("and it must reach past their far edge; fill %s, wrapped word "
                        + "%.1f..%.1f", chip, wrapped.left(), wrapped.right())
                .isGreaterThan(wrapped.right() - TOLERANCE);

        // A fill covering the whole line would satisfy both of the above.
        assertThat(chip.left())
                .describedAs("the fill wraps one run, not the line; the Hebrew runs "
                        + "%.1f..%.1f", hebrew.left(), hebrew.right())
                .isGreaterThan(hebrew.right() - TOLERANCE);
    }

    @Test
    void theSameLineLeftToRightPutsItsChipOnTheSameWords() throws Exception {
        // The control. Were this to fail as well, the fault would be in how the fill is
        // placed rather than in the reordering, and the failure above would name the wrong
        // cause.
        byte[] pdf = render(TextDirection.LTR, CHIP, REST);
        Extent chip = fillExtent(pdf);
        Span wrapped = drawnSpan(pdf, RtlChipTest::isLatin);

        assertThat(chip.left())
                .describedAs("fill %s, wrapped word %.1f..%.1f",
                        chip, wrapped.left(), wrapped.right())
                .isLessThan(wrapped.left() + TOLERANCE);
        assertThat(chip.right()).isGreaterThan(wrapped.right() - TOLERANCE);
    }

    @Test
    void aChipOfRightToLeftTextIsReversedInsideItsFill() throws Exception {
        // A chip is one unsplittable span: it is reversed whole or not at all. Mixed
        // directions inside one keep their logical order, which is a known approximation —
        // what has to hold is that a chip that is entirely right-to-left is reversed.
        //
        // The trailing words share no letter with the chip, which soleGlyphX enforces: a
        // letter appearing on both sides would otherwise be measured wherever it happened
        // to be found, and the reading would be about the wrong word without saying so.
        byte[] pdf = render(TextDirection.RTL, "שלום", " ברכת הבית");

        double first = soleGlyphX(pdf, 0x05E9);   // ש, the chip's first letter
        double last = soleGlyphX(pdf, 0x05DD);    // ם, its last

        assertThat(first)
                .describedAs("the chip's first letter is drawn at its right edge, so a chip "
                        + "of right-to-left text reads the way it was written. First letter "
                        + "at %.1f, last at %.1f", first, last)
                .isGreaterThan(last);

        // And the fill wraps them rather than sitting beside them.
        Extent chip = fillExtent(pdf);
        assertThat(chip.left())
                .describedAs("fill %s, chip letters %.1f..%.1f", chip, last, first)
                .isLessThan(last + TOLERANCE);
        assertThat(chip.right()).isGreaterThan(first - TOLERANCE);
    }

    /** The horizontal extent of the chip's fill, in points, found by its colour. */
    private static Extent fillExtent(byte[] pdf) throws IOException {
        BufferedImage image = rasterise(pdf);
        double left = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        int painted = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                // Full-strength fill only: an antialiased corner reads as a paler red, and
                // including it would report an extent the fill does not actually cover.
                if (red > 180 && green < 70 && blue < 70) {
                    painted++;
                    left = Math.min(left, x);
                    right = Math.max(right, x + 1.0);
                }
            }
        }
        assertThat(painted).describedAs("the chip's fill reached the page").isGreaterThan(0);
        return new Extent(left, right);
    }

    /**
     * Where a letter that occurs once on the page was drawn, in points.
     *
     * <p>Asking for a letter that occurs twice is a mistake this makes loud rather than
     * silent. Hebrew words share letters freely, and a measurement that quietly takes the
     * leftmost of several is a reading about a word nobody asked about.</p>
     */
    private static double soleGlyphX(byte[] pdf, int codePoint) throws IOException {
        List<DrawnGlyphs.Glyph> hits = DrawnGlyphs.matching(pdf, candidate -> candidate == codePoint);
        assertThat(hits)
                .describedAs("the letter U+%04X has to occur once for its position to mean "
                        + "anything", codePoint)
                .hasSize(1);
        return hits.get(0).left();
    }

    /** Where the glyphs standing for characters a predicate accepts were drawn, in points. */
    private static Span drawnSpan(byte[] pdf, IntPredicate accepts) throws IOException {
        List<DrawnGlyphs.Glyph> hits = DrawnGlyphs.matching(pdf, accepts);
        assertThat(hits).describedAs("the glyphs reached the page").isNotEmpty();

        double left = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        for (DrawnGlyphs.Glyph glyph : hits) {
            left = Math.min(left, glyph.left());
            right = Math.max(right, glyph.right());
        }
        return new Span(left, right);
    }

    private static boolean isLatin(int codePoint) {
        return codePoint >= 'A' && codePoint <= 'Z';
    }

    private static boolean isHebrew(int codePoint) {
        return codePoint >= 0x05D0 && codePoint <= 0x05EA;
    }

    /** One line: a chip, then the rest, both set in the family that covers both scripts. */
    private static byte[] render(TextDirection direction, String chipText, String restText) {
        DocumentTextStyle style = DocumentTextStyle.builder()
                .fontName(FontName.DAVID_LIBRE).size(18)
                .color(DocumentColor.rgb(0, 0, 0)).build();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 110)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .rich(rich -> rich
                            .highlight(chipText, style, FILL, 4, DocumentInsets.of(4))
                            .style(restText, style))
                    .direction(direction)
                    .textStyle(style)));

            return document.toPdfBytes();
        }
    }

    /** One point per pixel, so extents compare with the extractor's without a scale. */
    private static BufferedImage rasterise(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFRenderer(document).renderImageWithDPI(0, 72);
        }
    }

    /** Horizontal extent of the chip's fill. */
    private record Extent(double left, double right) {
        @Override
        public String toString() {
            return String.format("%.1f..%.1f", left, right);
        }
    }

    /** Horizontal extent of drawn glyphs. */
    private record Span(double left, double right) {
    }
}
