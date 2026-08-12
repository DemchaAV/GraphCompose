package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds a first-line indent to the edge the paragraph actually starts from.
 *
 * <p>The indent is a text prefix: spaces are prepended to the line's characters, and the
 * line is then placed by its width. That is a left-to-right shape of thinking, and the
 * question it leaves open is which edge the space ends up against once the line is
 * reordered — an indent that pushed a right-to-left paragraph away from the <em>left</em>
 * margin would leave the first line looking longer than the rest rather than indented.</p>
 *
 * <p>It does not, and this pins that. The measurement skips whitespace deliberately: the
 * prefix spaces are real drawn glyphs, so asking where the line's first {@code TextPosition}
 * sits answers where the padding is, not where the text is. That is what made an earlier
 * look at this seem to show the indent being measured and then dropped.</p>
 */
class RtlFirstLineIndentTest {

    private static final String HEBREW =
            "הטקסט העברי נקרא מימין לשמאל, והמנוע מסדר כל שורה בנפרד לפי האלגוריתם "
            + "הדו־כיווני של יוניקוד, והפסקה מתחילה מהקצה הימני של העמודה תמיד.";

    private static final String LATIN =
            "The Latin text reads left to right, and the paragraph starts at the left "
            + "edge of its column on every line unless something indents the first one.";

    /** Page 300 wide with 20pt margins, so the column runs from 20 to 280. */
    private static final double LEFT_EDGE = 20.0;
    private static final double RIGHT_EDGE = 280.0;
    private static final double TOLERANCE = 1.0;

    @Test
    void aRightToLeftFirstLineIsIndentedFromTheRightEdge() throws Exception {
        List<Extent> lines = drawnLines(TextDirection.RTL, true);

        assertThat(lines).describedAs("the paragraph wraps, so there is a line to compare against")
                .hasSizeGreaterThan(1);
        assertThat(lines.get(0).right)
                .describedAs("a right-to-left paragraph begins at the right edge, so that is the "
                        + "edge its first line has to be indented from; lines were %s", lines)
                .isLessThan(RIGHT_EDGE - TOLERANCE);
        assertThat(lines.get(1).right)
                .describedAs("and the lines after it keep touching that edge")
                .isGreaterThan(RIGHT_EDGE - TOLERANCE);
    }

    @Test
    void aLeftToRightFirstLineIsIndentedFromTheLeftEdge() throws Exception {
        List<Extent> lines = drawnLines(TextDirection.LTR, true);

        assertThat(lines).hasSizeGreaterThan(1);
        assertThat(lines.get(0).left)
                .describedAs("the control: same strategy, other edge; lines were %s", lines)
                .isGreaterThan(LEFT_EDGE + TOLERANCE);
        assertThat(lines.get(1).left).isLessThan(LEFT_EDGE + TOLERANCE);
    }

    @Test
    void withoutTheStrategyEveryLineStartsFromTheSameEdge() throws Exception {
        // Without this, an indent that never applied at all would satisfy nothing above
        // and still look like a passing suite.
        for (Extent line : drawnLines(TextDirection.RTL, false)) {
            assertThat(line.right)
                    .describedAs("no indent asked for, so every line reaches the right edge")
                    .isGreaterThan(RIGHT_EDGE - TOLERANCE);
        }
    }

    /** The horizontal extent of each line's visible glyphs, in the order they were drawn. */
    private static List<Extent> drawnLines(TextDirection direction, boolean indented)
            throws IOException {

        byte[] pdf;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 220)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(paragraph -> {
                paragraph.text(direction == TextDirection.RTL ? HEBREW : LATIN)
                        .direction(direction)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(direction == TextDirection.RTL
                                        ? FontName.DAVID_LIBRE : FontName.HELVETICA)
                                .size(12)
                                .build());
                if (indented) {
                    // The indent's size is the measured width of this prefix; the strategy
                    // alone indents by nothing.
                    paragraph.bulletOffset("        ")
                            .indentStrategy(DocumentTextIndent.FIRST_LINE);
                }
            }));

            pdf = document.toPdfBytes();
        }

        List<Extent> lines = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) throws IOException {
                    double left = Double.MAX_VALUE;
                    double right = -Double.MAX_VALUE;
                    for (TextPosition position : positions) {
                        if (position.getUnicode().isBlank()) {
                            continue;
                        }
                        left = Math.min(left, position.getXDirAdj());
                        right = Math.max(right, position.getXDirAdj() + position.getWidthDirAdj());
                    }
                    if (left < Double.MAX_VALUE) {
                        lines.add(new Extent(left, right));
                    }
                    super.writeString(text, positions);
                }
            }.getText(document);
        }
        return lines;
    }

    /** Where a line's visible glyphs begin and end. */
    private record Extent(double left, double right) {
        @Override
        public String toString() {
            return String.format("%.1f..%.1f", left, right);
        }
    }
}
