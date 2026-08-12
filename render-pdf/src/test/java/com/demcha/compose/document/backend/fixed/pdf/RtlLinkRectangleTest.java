package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds a hyperlink to the words it was written on, in a line that gets reordered.
 *
 * <p>A clickable rectangle is a separate object from the text. Nothing in the PDF ties the
 * two together: the glyphs are painted in one pass and the annotation is placed by its own
 * arithmetic, so the two can disagree and the file stays valid. In a left-to-right line
 * they cannot drift far, because both walk the spans in the same order. A right-to-left
 * line is where they come apart — the glyphs are drawn in visual order and a rectangle
 * placed by walking the logical one lands where the span <em>would have been</em> if the
 * line ran the other way.</p>
 *
 * <p>That failure is silent in every way that usually catches a bug. The page looks right,
 * the text extracts right, the link opens the right address — it is simply attached to
 * whichever words happen to sit where the annotation ended up. So this test does not check
 * that a rectangle exists; it checks that the rectangle covers the drawn position of the
 * word the link was written on, and misses the words it was not.</p>
 */
class RtlLinkRectangleTest {

    /**
     * Hebrew, a linked Latin product name, more Hebrew.
     *
     * <p>The linked run is Latin on purpose, and not only because it is the realistic case.
     * Hebrew words share letters, so glyphs cannot be attributed to a word by their
     * characters; a Latin run inside Hebrew is identifiable glyph by glyph, which is what
     * lets this test say where the linked word was actually drawn.</p>
     */
    private static final String LINKED = "GraphCompose";
    private static final String AFTER = " שלום עולם ועוד מילים";

    private static final DocumentTextStyle HEBREW = DocumentTextStyle.builder()
            .fontName(FontName.DAVID_LIBRE).size(18).build();

    @Test
    void theLinkRectangleCoversTheWordItWasWrittenOnInARightToLeftLine() throws Exception {
        byte[] pdf = render(TextDirection.RTL);

        PDRectangle rect = onlyLinkRectangle(pdf);
        Span linked = drawnLatinSpan(pdf);
        Span hebrew = drawnHebrewSpan(pdf);

        assertThat(linked.left)
                .describedAs("the link rectangle must start where the linked word is drawn, "
                        + "not where it would sit in a left-to-right line. Rectangle "
                        + "%.1f..%.1f, linked word %.1f..%.1f, the other words at "
                        + "%.1f..%.1f",
                        rect.getLowerLeftX(), rect.getUpperRightX(), linked.left, linked.right,
                        hebrew.left, hebrew.right)
                .isGreaterThanOrEqualTo(rect.getLowerLeftX() - 1.0);
        assertThat(linked.right).isLessThanOrEqualTo(rect.getUpperRightX() + 1.0);

        // And it must not have swallowed the line: a rectangle spanning everything would
        // satisfy the check above while making every word clickable.
        assertThat((double) rect.getWidth())
                .describedAs("the rectangle covers the linked word, not the whole line")
                .isLessThan(hebrew.right - hebrew.left);
    }

    @Test
    void theSameLineLeftToRightPutsItsRectangleOnTheSameWord() throws Exception {
        // The control. If this one failed too, the fault would be in the rectangle
        // arithmetic rather than in the reordering, and the failure above would be
        // pointing at the wrong thing.
        byte[] pdf = render(TextDirection.LTR);

        PDRectangle rect = onlyLinkRectangle(pdf);
        Span linked = drawnLatinSpan(pdf);

        assertThat(linked.left).isGreaterThanOrEqualTo(rect.getLowerLeftX() - 1.0);
        assertThat(linked.right).isLessThanOrEqualTo(rect.getUpperRightX() + 1.0);
    }

    private static byte[] render(TextDirection direction) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(360, 120)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    // The link is the FIRST run on purpose. In a right-to-left line the
                    // first logical span is drawn at the RIGHT edge, so a rectangle placed
                    // by walking the logical order lands at the left — the widest possible
                    // gap between the two orders, and the one a reader would notice least.
                    .rich(rich -> rich
                            .link(LINKED, "https://example.com/rtl")
                            .style(AFTER, HEBREW))
                    .direction(direction)
                    .textStyle(HEBREW)));

            return document.toPdfBytes();
        }
    }

    /** The one link annotation on the page; more than one would make the assertions ambiguous. */
    private static PDRectangle onlyLinkRectangle(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage page = document.getPage(0);
            List<PDRectangle> rectangles = new ArrayList<>();
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link) {
                    rectangles.add(link.getRectangle());
                }
            }
            assertThat(rectangles)
                    .describedAs("the paragraph carries exactly one link")
                    .hasSize(1);
            return rectangles.get(0);
        }
    }

    /** Where the linked Latin run was drawn, in the page's coordinates. */
    private static Span drawnLatinSpan(byte[] pdf) throws IOException {
        return drawnSpanMatching(pdf, ch -> ch >= 'A' && ch <= 'z');
    }

    /** Where the Hebrew around it was drawn — the extent the rectangle must not cover. */
    private static Span drawnHebrewSpan(byte[] pdf) throws IOException {
        return drawnSpanMatching(pdf, ch -> ch >= 0x05D0 && ch <= 0x05EA);
    }

    /** Where the glyphs standing for characters a predicate accepts were actually drawn. */
    private static Span drawnSpanMatching(byte[] pdf, java.util.function.IntPredicate accepts)
            throws IOException {

        // Each glyph is attributed by what the font says it stands for, not by the order
        // it was painted in — the line is reordered, so the sequence says nothing.
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

    /** Horizontal extent of a drawn word. */
    private record Span(double left, double right) {
    }
}
