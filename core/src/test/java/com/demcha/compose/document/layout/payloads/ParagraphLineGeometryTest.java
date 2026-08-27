package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.TextAlign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one expression three backends share.
 *
 * <p>It had been copied into each of them, which is what makes it worth a test of its
 * own: the failure of a drifted copy is not a crash but a link that no longer covers its
 * text, or a slide whose words sit a few points from where the page put them.</p>
 */
class ParagraphLineGeometryTest {

    private static final double INNER_X = 40.0;
    private static final double INNER_WIDTH = 200.0;
    private static final double LINE_WIDTH = 50.0;

    @Test
    void leftStartsAtTheContentEdge() {
        assertThat(ParagraphLineGeometry.lineStartX(TextAlign.LEFT, INNER_X, INNER_WIDTH, LINE_WIDTH))
                .isEqualTo(40.0);
    }

    @Test
    void rightEndsAtTheContentEdge() {
        assertThat(ParagraphLineGeometry.lineStartX(TextAlign.RIGHT, INNER_X, INNER_WIDTH, LINE_WIDTH))
                .describedAs("the line's right edge lands on the content box's right edge")
                .isEqualTo(190.0);
    }

    @Test
    void centreLeavesEqualSpaceOnBothSides() {
        double x = ParagraphLineGeometry.lineStartX(TextAlign.CENTER, INNER_X, INNER_WIDTH, LINE_WIDTH);

        assertThat(x - INNER_X)
                .isEqualTo(INNER_X + INNER_WIDTH - (x + LINE_WIDTH));
    }

    @Test
    void aLineWiderThanTheBoxOverflowsInTheDirectionItIsAlignedTo() {
        assertThat(ParagraphLineGeometry.lineStartX(TextAlign.RIGHT, INNER_X, 100.0, 160.0))
                .describedAs("clamping here would silently disagree with the width the layout "
                        + "already committed to, so the overflow is left visible")
                .isEqualTo(-20.0);
    }

    @Test
    void aMissingAlignmentBehavesAsLeft() {
        assertThat(ParagraphLineGeometry.lineStartX(null, INNER_X, INNER_WIDTH, LINE_WIDTH))
                .isEqualTo(ParagraphLineGeometry.lineStartX(TextAlign.LEFT, INNER_X, INNER_WIDTH, LINE_WIDTH));
    }

    // ------------------------------------------------------------ the vertical walk ---

    @Test
    void contentStartsBelowTheTopPadding() {
        // The fragment box is measured from its bottom, so its top is y + height and the
        // padding comes off that. Getting the sign wrong here puts every line of every
        // paragraph one padding out, in the same direction, which reads as a global
        // offset rather than as a bug in one expression.
        assertThat(ParagraphLineGeometry.contentTop(100.0, 60.0, 8.0)).isEqualTo(152.0);
    }

    @Test
    void linesStackDownward() {
        double first = ParagraphLineGeometry.contentTop(100.0, 60.0, 0.0);
        double second = ParagraphLineGeometry.nextLineTop(first, 12.0, 4.0);

        assertThat(second)
                .describedAs("y grows upward, so the next line's top is lower")
                .isEqualTo(first - 16.0);
        assertThat(second).isLessThan(first);
    }

    @Test
    void theBaselineSitsInsideItsOwnLineBox() {
        double lineTop = 200.0;
        double lineHeight = 14.0;
        double descent = 3.0;

        double baseline = ParagraphLineGeometry.baselineY(lineTop, lineHeight, descent);

        assertThat(baseline).isEqualTo(189.0);
        assertThat(baseline)
                .describedAs("a baseline outside its box would draw the line into its neighbour")
                .isBetween(lineTop - lineHeight, lineTop);
    }

    @Test
    void aBaselineIsMeasuredFromTheLineBottom() {
        // The descent is the whole of the offset: two lines of the same height with
        // different descents share a box top and sit at different baselines.
        double lineTop = 200.0;

        assertThat(ParagraphLineGeometry.baselineY(lineTop, 14.0, 5.0)
                - ParagraphLineGeometry.baselineY(lineTop, 14.0, 3.0))
                .isEqualTo(2.0);
    }

    @Test
    void aGaplessStackLeavesNoSpaceBetweenLines() {
        double first = 300.0;
        double second = ParagraphLineGeometry.nextLineTop(first, 12.0, 0.0);

        assertThat(second)
                .describedAs("the next line's top meets the previous line's bottom exactly")
                .isEqualTo(first - 12.0);
    }
}
