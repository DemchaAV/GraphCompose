package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds a chip's interior to the order and forms its own levels are entitled to.
 *
 * <p>A chip is one rounded fill, so the wrapper cannot split it at a level boundary and
 * it reaches the renderer whole, carrying its first character's level. Reversed whole —
 * as it was — a chip reading {@code (a > b)} after a Hebrew word drew as
 * {@code (b < a)}: operands swapped, comparison flipped, meaning inverted on the page.
 * The interior of that chip is left-to-right text, which UAX #9 neither reorders nor
 * mirrors; only the enclosing brackets belong to the level that moves.</p>
 *
 * <p>The assertions read the drawn glyphs by position ({@link DrawnGlyphs}), not the
 * extracted text — extraction is routed through {@code ActualText} and the font's
 * {@code ToUnicode} map, both of which deliberately answer with what the author wrote
 * and would report the same string whichever order the page drew.</p>
 */
class PdfRtlChipTest {

    @Test
    void aMixedChipKeepsItsOperandsInReadingOrder() throws Exception {
        List<DrawnGlyphs.Glyph> glyphs = DrawnGlyphs.leftToRight(renderChipLine("(a > b)"));

        int a = indexOfFirst(glyphs, "a");
        int b = indexOfFirst(glyphs, "b");
        assertThat(a).describedAs("the chip's 'a' is on the page; glyphs were %s", glyphs)
                .isNotNegative();
        assertThat(b).describedAs("the chip's 'b' is on the page").isNotNegative();
        assertThat(a)
                .describedAs("'a' must be drawn left of 'b' — reversed whole, the chip "
                        + "put its operands the other way round and the page said the "
                        + "opposite of what the author wrote; glyphs were %s", glyphs)
                .isLessThan(b);
    }

    @Test
    void digitsInsideAHebrewChipStayForward() throws Exception {
        // The same failure without brackets: a chip holding Hebrew and a number.
        // Digits resolve to the left-to-right level and read forwards in Hebrew text;
        // reversed whole, the year came out backwards.
        List<DrawnGlyphs.Glyph> glyphs = DrawnGlyphs.leftToRight(renderChipLine("שנה 2026"));

        int two = indexOfFirst(glyphs, "2");
        int zero = indexOfFirst(glyphs, "0");
        int six = indexOfFirst(glyphs, "6");
        assertThat(two).describedAs("the digits are on the page; glyphs were %s", glyphs)
                .isNotNegative();
        assertThat(two).describedAs("2 before 0, as written").isLessThan(zero);
        assertThat(zero).describedAs("0 before 6, as written").isLessThan(six);
    }

    private static int indexOfFirst(List<DrawnGlyphs.Glyph> glyphs, String character) {
        for (int index = 0; index < glyphs.size(); index++) {
            if (glyphs.get(index).character().equals(character)) {
                return index;
            }
        }
        return -1;
    }

    private static byte[] renderChipLine(String chipText) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 140)
                .margin(DocumentInsets.of(20))
                .create()) {

            // highlight() rather than chip(), so the chip run carries the
            // Hebrew-capable font itself — chip() leaves the run's font to the
            // default, and Hebrew sanitized into '?' would sidestep the reorder
            // this test exists to pin.
            document.pageFlow(page -> page.addParagraph(p -> p
                    .rich(rich -> rich.plain("שלום ").highlight(chipText,
                            DocumentTextStyle.builder()
                                    .fontName(FontName.DAVID_LIBRE).size(15).build(),
                            DocumentColor.rgb(0xEF, 0xF1, 0xF3),
                            3, DocumentInsets.of(2)))
                    .direction(TextDirection.RTL)
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.DAVID_LIBRE).size(15).build())));

            return document.toPdfBytes();
        }
    }
}
