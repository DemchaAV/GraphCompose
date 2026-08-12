package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
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
 * Holds an underline under the words it was written on, in a line that gets reordered.
 *
 * <p>A decoration is drawn separately from the glyphs it belongs to: the run is measured and
 * a mark is painted across the width it occupies. That leaves the opening a hyperlink
 * rectangle has — the mark's position is arithmetic, and arithmetic done over the logical
 * order puts it where the run <em>would have been</em> if the line ran the other way. In a
 * right-to-left line that is the far end, so the mark would sit under different words
 * entirely while still looking like a properly drawn underline.</p>
 *
 * <p>The mark is isolated by rendering the same line twice, with the decoration and without,
 * and taking the ink one has that the other does not. That only means something if nothing
 * else moved between the two, so the test establishes it rather than assuming it: no ink may
 * <em>disappear</em> when the decoration is asked for. If a glyph shifted, that check fails
 * and says so, instead of letting the measurement quietly describe a glyph edge.</p>
 *
 * <p>Rendered at 72 dpi, so a pixel is a point and the mark's extent compares with the glyph
 * positions the text extractor reports without a scale factor in between.</p>
 */
class RtlUnderlineTest {

    /**
     * Hebrew with an underlined Latin word, and the Latin is what carries the mark.
     *
     * <p>Hebrew words share letters, so a glyph cannot be attributed to a word by what it is;
     * a Latin run inside Hebrew can be, which is what lets the test say where the underlined
     * word was actually drawn. Capitals only, so nothing descends below the baseline and the
     * mark is the lowest ink on the line rather than something tangled with a tail.</p>
     */
    private static final String UNDERLINED = "GRAPHCOMPOSE";
    private static final String REST = " שלום עולם ועוד מילים";

    /** Room for the mark to run the width of the run rather than the width of its ink. */
    private static final double TOLERANCE = 3.0;

    /** Dark enough to be ink rather than an antialiased edge. */
    private static final int INK = 120;

    @Test
    void theMarkSitsUnderTheWordItWasWrittenOnInARightToLeftLine() throws Exception {
        Extent mark = markExtent(TextDirection.RTL);
        Span underlined = drawnSpan(TextDirection.RTL, RtlUnderlineTest::isLatin);
        Span hebrew = drawnSpan(TextDirection.RTL, RtlUnderlineTest::isHebrew);

        assertThat(mark.left())
                .describedAs("the mark must begin where the underlined word is drawn, not "
                        + "where it would sit in a left-to-right line. Mark %s, underlined "
                        + "word %.1f..%.1f, the other words at %.1f..%.1f",
                        mark, underlined.left(), underlined.right(), hebrew.left(), hebrew.right())
                .isGreaterThan(underlined.left() - TOLERANCE);
        assertThat(mark.right())
                .describedAs("and end there; mark %s, underlined word %.1f..%.1f",
                        mark, underlined.left(), underlined.right())
                .isLessThan(underlined.right() + TOLERANCE);

        // A mark spanning the whole line fails the first check, but one spanning the Hebrew
        // and the Latin together could pass it.
        assertThat(mark.left())
                .describedAs("the mark covers one word, not the line; the Hebrew runs "
                        + "%.1f..%.1f", hebrew.left(), hebrew.right())
                .isGreaterThan(hebrew.right() - TOLERANCE);
    }

    @Test
    void theSameLineLeftToRightPutsItsMarkOnTheSameWord() throws Exception {
        // The control. Were this to fail too, the fault would be in how the mark is placed
        // rather than in the reordering, and the failure above would name the wrong cause.
        Extent mark = markExtent(TextDirection.LTR);
        Span underlined = drawnSpan(TextDirection.LTR, RtlUnderlineTest::isLatin);

        assertThat(mark.left())
                .describedAs("mark %s, underlined word %.1f..%.1f",
                        mark, underlined.left(), underlined.right())
                .isGreaterThan(underlined.left() - TOLERANCE);
        assertThat(mark.right()).isLessThan(underlined.right() + TOLERANCE);
    }

    @Test
    void theMarkIsTheLowestInkOnARightToLeftLine() throws Exception {
        // Under the right word but at the wrong height, a mark still passes the checks
        // above. Neither script here descends, so the mark has to be the line's bottom.
        BufferedImage plain = rasterise(render(TextDirection.RTL, false));
        BufferedImage marked = rasterise(render(TextDirection.RTL, true));

        assertThat(lowestInkRow(marked))
                .describedAs("the mark extends the line's ink downward")
                .isGreaterThan(lowestInkRow(plain));
    }

    /** The horizontal extent of the ink the decoration added, in points. */
    private static Extent markExtent(TextDirection direction) throws IOException {
        BufferedImage plain = rasterise(render(direction, false));
        BufferedImage marked = rasterise(render(direction, true));

        assertThat(marked.getWidth()).isEqualTo(plain.getWidth());
        assertThat(marked.getHeight()).isEqualTo(plain.getHeight());

        double left = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        int added = 0;
        int lost = 0;
        for (int y = 0; y < marked.getHeight(); y++) {
            for (int x = 0; x < marked.getWidth(); x++) {
                boolean inkNow = isInk(marked, x, y);
                boolean inkBefore = isInk(plain, x, y);
                if (inkBefore && !inkNow) {
                    lost++;
                } else if (inkNow && !inkBefore) {
                    added++;
                    left = Math.min(left, x);
                    right = Math.max(right, x + 1.0);
                }
            }
        }

        // The premise of the measurement: asking for a decoration adds ink and moves none.
        // Were a glyph to shift, what follows would be describing a glyph edge.
        assertThat(lost)
                .describedAs("ink disappeared when the decoration was asked for, so the run "
                        + "moved and the difference between the two renders is no longer "
                        + "just the mark")
                .isZero();
        assertThat(added).describedAs("the decoration reached the page").isGreaterThan(0);
        return new Extent(left, right);
    }

    /** Where the glyphs standing for characters a predicate accepts were drawn, in points. */
    private static Span drawnSpan(TextDirection direction, IntPredicate accepts) throws IOException {
        List<DrawnGlyphs.Glyph> hits = DrawnGlyphs.matching(render(direction, true), accepts);
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

    /**
     * The same line twice, differing only in whether the Latin run is underlined.
     *
     * <p>Both runs are set in the same family on purpose: a font change between the two
     * renders would move the glyphs and cost the difference its meaning.</p>
     */
    private static byte[] render(TextDirection direction, boolean underlined) {
        DocumentTextStyle plain = DocumentTextStyle.builder()
                .fontName(FontName.DAVID_LIBRE).size(18).build();
        DocumentTextStyle marked = DocumentTextStyle.builder()
                .fontName(FontName.DAVID_LIBRE).size(18)
                .decoration(DocumentTextDecoration.UNDERLINE).build();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 110)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    // Underlined run first on purpose: in a right-to-left line the first
                    // logical run is drawn at the right edge, so a mark placed by walking
                    // the logical order lands at the left — the widest the two orders can
                    // disagree, and the reading a glance is least likely to catch.
                    .rich(rich -> rich
                            .style(UNDERLINED, underlined ? marked : plain)
                            .style(REST, plain))
                    .direction(direction)
                    .textStyle(plain)));

            return document.toPdfBytes();
        }
    }

    /** One point per pixel, so extents compare with the extractor's without a scale. */
    private static BufferedImage rasterise(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFRenderer(document).renderImageWithDPI(0, 72);
        }
    }

    private static boolean isInk(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        return ((((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3) < INK;
    }

    private static int lowestInkRow(BufferedImage image) {
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isInk(image, x, y)) {
                    return y;
                }
            }
        }
        return -1;
    }

    /** Horizontal extent of the added mark. */
    private record Extent(double left, double right) {
        @Override
        public String toString() {
            return String.format("%.1f..%.1f", left, right);
        }
    }

    /** Horizontal extent of a drawn word. */
    private record Span(double left, double right) {
    }
}
